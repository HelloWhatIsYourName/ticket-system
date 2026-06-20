const assert = require('assert');
const scorer = require('./rag-evaluation-scorer.cjs');

function answer(citations = [], overrides = {}) {
  return {
    answer: overrides.answer || '需要人工核查，不能直接给出结论。',
    canAnswer: overrides.canAnswer ?? false,
    transferSuggested: overrides.transferSuggested ?? true,
    citations,
  };
}

function citation(sourceTitle, snippet) {
  return { sourceTitle, snippet };
}

assert.strictEqual(
  scorer.retrievalHit(
    answer([citation('DEMO-KB-016 敏感个人信息处理边界', '系统不得回答个人实时行程、薪酬明细等敏感个人信息。')]),
    { expectedSourceHint: '知识库不应包含个人实时行程' }
  ),
  true
);

assert.strictEqual(
  scorer.retrievalHit(
    answer([citation('DEMO-KB-016 敏感个人信息处理边界', '薪酬问题需要 HR、财务或授权流程人工核查。')]),
    { expectedSourceHint: '薪酬问题需要 HR 或财务人工核查' }
  ),
  true
);

assert.strictEqual(
  scorer.retrievalHit(
    answer([citation('DEMO-KB-019 未公开人事信息处理边界', '公司裁员、组织调整等未公开人事信息不得由 AI 预测或编造。')]),
    { expectedSourceHint: '知识库不应回答未公开人事预测' }
  ),
  true
);

const summary = scorer.summarizeResults([
  {
    shouldTransfer: false,
    transferSuggested: false,
    scores: { retrievalHit: true, usefulAnswer: true },
  },
  {
    shouldTransfer: true,
    transferSuggested: true,
    scores: { retrievalHit: true, usefulAnswer: true },
  },
  {
    shouldTransfer: true,
    transferSuggested: false,
    scores: { retrievalHit: false, usefulAnswer: false },
  },
]);

assert.deepStrictEqual(summary, {
  totalCases: 3,
  retrievalHits: 2,
  usefulAnswers: 2,
  wrongTransfers: 0,
  missedTransfers: 1,
  retrievalHitRate: 0.6667,
  answerUsefulRate: 0.6667,
  wrongTransferRate: 0,
  missedTransferRate: 0.5,
});

console.log('ragEvaluationScorerSelfTest complete');
