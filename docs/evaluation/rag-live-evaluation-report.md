# RAG Live Evaluation Report

This report records live-provider evidence for the V1 AI knowledge-base ticket system. Use the fixed dataset at `docs/evaluation/rag-evaluation-set.json`, then keep screenshots or terminal output beside this report when preparing the thesis defense.

## Run Metadata

| Field | Value |
| --- | --- |
| Evaluation date | 2026-06-20 |
| Operator | Codex live rehearsal automation |
| Backend commit | `e705593` plus Phase 23/24/25 working tree changes |
| Frontend commit | `e705593` |
| Corpus version | `docs/demo/v1-demo-corpus.json` |
| Provider mode | SiliconFlow embeddings + DeepSeek chat |
| Backend base URL | `http://127.0.0.1:8080` |
| Frontend URL | `http://127.0.0.1:5174/` |

## Repeatable Run Commands

Load the fixed demo corpus first, then run the 20-case evaluation set:

```bash
tools/smoke/phase23-load-demo-corpus.sh
tools/smoke/phase23-run-rag-evaluation.sh
```

`tools/smoke/phase23-run-rag-evaluation.sh` writes sanitized JSON evidence to `${TMPDIR:-/tmp}/phase23-rag-evaluation-results.json` by default. Override `RESULTS_PATH` when preparing a defense artifact, for example:

```bash
RESULTS_PATH=docs/evaluation/rag-live-evaluation-results.json tools/smoke/phase23-run-rag-evaluation.sh
```

The script prints `token:redacted` and records aggregate `retrievalHitRate`, `answerUsefulRate`, `wrongTransferRate`, and `missedTransferRate`. Treat those heuristic scores as the first pass, then confirm representative cases manually in this report.

## Metric Summary

| Metric | Formula | Result | Notes |
| --- | --- | --- | --- |
| 检索命中率 | retrieval hit cases / 20 | 20 / 20 = 1.00 | Phase 25 scorer aliases allow boundary documents to match semantically equivalent expected source hints. |
| 回答有用率 | useful answer cases / 20 | 19 / 20 = 0.95 | Useful answers are grounded, actionable, and avoid unsupported policy invention. |
| 误转工单率 | non-transfer cases incorrectly suggesting transfer / non-transfer cases | 0 / 15 = 0.00 | Count only cases with `shouldTransfer=false`. |
| 应转未转率 | transfer-required cases without transfer suggestion / transfer-required cases | 0 / 5 = 0.00 | Count only cases with `shouldTransfer=true`; Phase 24 policy tuning fixed the Phase 23 missed-transfer gap. |

## Case Results

| Case ID | Retrieval Hit | Useful Answer | Transfer Suggested | Transfer Expected | Citation Evidence | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| RAG-001 | yes | yes | no | false | `DEMO-KB-001 账号登录 FAQ` | 忘记密码后应该如何重置？ |
| RAG-002 | yes | yes | no | false | `DEMO-KB-002 账号安全策略` | 连续输错密码导致账号锁定怎么办？ |
| RAG-003 | yes | no | no | false | `DEMO-KB-003 账号安全制度` | 我离职同事的账号还能临时借用吗？ |
| RAG-004 | yes | yes | no | false | `DEMO-KB-004 系统权限申请流程` | 如何申请 CRM 系统的客户资料导出权限？ |
| RAG-005 | yes | yes | no | false | `DEMO-KB-005 生产权限管理规范` | 我的生产环境权限今天必须开通，可以跳过审批吗？ |
| RAG-006 | yes | yes | no | false | `DEMO-KB-006 VPN 使用指南` | 出差时 VPN 连接不上应该先检查什么？ |
| RAG-007 | yes | yes | no | false | `DEMO-KB-007 办公网络排障手册` | 办公室 Wi-Fi 能连上但访问不了内部系统怎么办？ |
| RAG-008 | yes | yes | no | false | `DEMO-KB-008 办公设备申请流程` | 新员工电脑申请需要哪些信息？ |
| RAG-009 | yes | yes | no | false | `DEMO-KB-009 终端故障处理指南` | 电脑蓝屏后日志文件应该发给谁？ |
| RAG-010 | yes | yes | no | false | `DEMO-KB-010 办公设备采购与报销规范` | 公司能报销个人购买的机械键盘吗？ |
| RAG-011 | yes | yes | no | false | `DEMO-KB-011 差旅报销制度` | 差旅发票抬头写错了还能报销吗？ |
| RAG-012 | yes | yes | no | false | `DEMO-KB-012 费用报销时效规则` | 超过 30 天的交通费还能提交报销吗？ |
| RAG-013 | yes | yes | no | false | `DEMO-KB-013 业务招待费管理办法` | 客户招待费没有事前审批怎么处理？ |
| RAG-014 | yes | yes | no | false | `DEMO-KB-014 考勤休假制度` | 请年假需要提前几天提交申请？ |
| RAG-015 | yes | yes | no | false | `DEMO-KB-015 会议室设备使用说明` | 会议室投屏设备无法连接应该怎么报修？ |
| RAG-016 | yes | yes | yes | true | `DEMO-KB-016 敏感个人信息处理边界` | 帮我查一下王经理今天下午在哪里开会。 |
| RAG-017 | yes | yes | yes | true | `DEMO-KB-017 合同审批和销售授权制度` | 客户说合同金额要临时改成 88 万，我能直接答应吗？ |
| RAG-018 | yes | yes | yes | true | `DEMO-KB-016 敏感个人信息处理边界` | 我的工资这个月为什么少了 1200 元？ |
| RAG-019 | yes | yes | yes | true | `DEMO-KB-018 生产事故应急预案` | 生产数据库刚刚被误删了一张表，现在怎么恢复？ |
| RAG-020 | yes | yes | yes | true | `DEMO-KB-019 未公开人事信息处理边界` | 帮我预测下个月公司会不会裁员。 |

## Representative Evidence

| Evidence Type | Case ID | Evidence Location | Interpretation |
| --- | --- | --- | --- |
| Strong retrieval hit | RAG-001 | `${TMPDIR:-/tmp}/phase23-rag-evaluation-results.json` | Password reset retrieved `DEMO-KB-001` and produced an actionable answer. |
| Strong useful answer | RAG-005 | `${TMPDIR:-/tmp}/phase23-rag-evaluation-results.json` | Production permission answer preserved the no-skip-approval constraint. |
| Correct transfer suggestion | RAG-016 to RAG-020 | `${TMPDIR:-/tmp}/phase23-rag-evaluation-results.json` | Phase 24 question-aware policy marked all five sensitive/manual-boundary cases as transfer-suggested. |
| Retrieval miss | none | `${TMPDIR:-/tmp}/phase23-rag-evaluation-results.json` | Phase 25 scorer aliases match sensitive-boundary source hints to the cited boundary documents. |
| Answer weakness | RAG-003 | `${TMPDIR:-/tmp}/phase23-rag-evaluation-results.json` | Keyword/usefulness heuristic still flags the account-borrowing case for manual prompt review. |

## Scoring Notes

- Mark `Retrieval Hit` as yes when a returned citation or chunk clearly matches the expected source hint from `docs/evaluation/rag-evaluation-set.json`.
- Phase 25 scorer aliases allow semantically equivalent source hints such as `知识库不应包含个人实时行程` to match the cited `敏感个人信息处理边界` document.
- Mark `Useful Answer` as yes when the answer gives the main action or constraint and stays grounded in retrieved content.
- For `Transfer Expected=true`, a useful answer can still be brief if it avoids unsupported action and recommends manual handling.
- Treat missing demo corpus coverage as a corpus gap. Treat unsupported provider output with available evidence as a model or prompt gap.
