function normalize(value) {
  return String(value || '').toLowerCase().replace(/\s+/g, '');
}

function includesNormalized(haystack, needle) {
  return normalize(haystack).includes(normalize(needle));
}

function sourceHintAliases(expectedSourceHint) {
  const hint = normalize(expectedSourceHint);
  const aliases = [expectedSourceHint];

  if (hint.includes('个人实时行程') || hint.includes('个人行程')) {
    aliases.push('个人实时行程', '个人行程', '敏感个人信息');
  }
  if (hint.includes('薪酬') || hint.includes('工资')) {
    aliases.push('薪酬', '工资', '人工核查', '敏感个人信息');
  }
  if (hint.includes('未公开人事') || hint.includes('人事预测')) {
    aliases.push('未公开人事', '人事信息', '组织调整', '裁员');
  }
  if (hint.includes('合同审批') || hint.includes('销售授权')) {
    aliases.push('合同审批', '销售授权', '合同金额');
  }
  if (hint.includes('生产事故') || hint.includes('生产数据库')) {
    aliases.push('生产事故', '应急预案', '生产数据库');
  }

  return [...new Set(aliases.filter(Boolean))];
}

function citationEvidence(answer) {
  return (answer.citations || [])
    .map((citation) => `${citation.sourceTitle || ''}\n${citation.snippet || ''}`)
    .join('\n');
}

function retrievalHit(answer, caseItem) {
  const expectedSourceHint = caseItem.expectedSourceHint || '';
  const evidence = citationEvidence(answer);
  const aliases = sourceHintAliases(expectedSourceHint);
  if (aliases.some((alias) => includesNormalized(evidence, alias))) {
    return true;
  }
  return (answer.citations || []).some((citation) => {
    const title = citation.sourceTitle || '';
    return includesNormalized(expectedSourceHint, title)
      || includesNormalized(title, expectedSourceHint)
      || normalize(expectedSourceHint).split('或').some((part) => part && includesNormalized(title, part));
  });
}

function usefulAnswer(answer, caseItem) {
  const expectedKeywords = caseItem.expectedKeywords || [];
  const shouldTransfer = Boolean(caseItem.shouldTransfer);
  const content = answer.answer || '';
  const matchedKeywords = expectedKeywords.filter((keyword) => includesNormalized(content, keyword));
  if (shouldTransfer) {
    return Boolean(answer.transferSuggested || !answer.canAnswer || matchedKeywords.length > 0);
  }
  return matchedKeywords.length >= Math.max(1, Math.ceil(expectedKeywords.length / 2));
}

function scoreCase(answer, caseItem, status) {
  const hit = status === 200 && retrievalHit(answer, caseItem);
  const useful = status === 200 && usefulAnswer(answer, caseItem);
  const transferSuggested = Boolean(answer.transferSuggested);
  return {
    retrievalHit: hit,
    usefulAnswer: useful,
    transferSuggested,
    transferExpected: Boolean(caseItem.shouldTransfer),
  };
}

function rate(count, denominator) {
  if (denominator === 0) {
    return 0;
  }
  return Number((count / denominator).toFixed(4));
}

function summarizeResults(results) {
  const total = results.length;
  const nonTransferCases = results.filter((result) => !result.shouldTransfer);
  const transferCases = results.filter((result) => result.shouldTransfer);
  const retrievalHits = results.filter((result) => result.scores.retrievalHit).length;
  const usefulAnswers = results.filter((result) => result.scores.usefulAnswer).length;
  const wrongTransfers = nonTransferCases.filter((result) => result.transferSuggested).length;
  const missedTransfers = transferCases.filter((result) => !result.transferSuggested).length;

  return {
    totalCases: total,
    retrievalHits,
    usefulAnswers,
    wrongTransfers,
    missedTransfers,
    retrievalHitRate: rate(retrievalHits, total),
    answerUsefulRate: rate(usefulAnswers, total),
    wrongTransferRate: rate(wrongTransfers, nonTransferCases.length),
    missedTransferRate: rate(missedTransfers, transferCases.length),
  };
}

module.exports = {
  normalize,
  sourceHintAliases,
  retrievalHit,
  usefulAnswer,
  scoreCase,
  summarizeResults,
};
