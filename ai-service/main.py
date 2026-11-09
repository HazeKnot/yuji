from fastapi import FastAPI
from pydantic import BaseModel

app = FastAPI(title="yuji-ai-service", version="0.1.0")

# 一期：规则版情绪风险分级（可解释、可审计）
# 二期：替换为多模态融合模型（文本 + 语音声学特征 + 量表 + 行为时序），见 docs/07 §5.1

NEGATIVE_WORDS = ["睡不着", "撑不住", "累", "哭", "没用", "难过", "焦虑", "崩溃", "孤独", "害怕"]
CRISIS_WORDS = ["不想活", "自杀", "自残", "伤害自己", "解脱", "消失"]


class EmotionReq(BaseModel):
    text: str = ""
    scale_scores: dict = {}      # 如 {"epds": 13}
    behavior_features: dict = {} # 如 {"consecutive_low_days": 5}


class ConsultReq(BaseModel):
    message: str = ""
    topic: str = ""


class ModerationReq(BaseModel):
    text: str = ""
    context: str = "community"  # community 帖子/评论 / checkin 情绪打卡


# ---------- AI 社区风控引擎（rules-moderate-v1） ----------
# 三类词典，可解释、可审计；上线后由人工审核样本持续回归词表。
# BLOCK：广告导流/违规交易 → 直接屏蔽进待审核
CRISIS_WORDS = ["不想活", "自杀", "自残", "想死", "伤害自己", "解脱", "消失", "跳楼", "割腕"]
BLOCK_WORDS = ["加微信", "加qq", "代购", "私聊卖", "药流", "打胎药", "刷单", "兼职日结",
               "扫码进群", "低价代做", "回收奶粉", "出售处方"]
# NEGATIVE：消极情绪表达 → 放行内容，但对人提交"消极预警"告警
NEGATIVE_WORDS = ["睡不着", "撑不住", "好累", "好想哭", "没用", "废物", "难过", "焦虑",
                  "崩溃", "孤独", "害怕", "压抑", "喘不过气", "没人管我", "撑不下去了",
                  "活着好累", "天天哭", "情绪崩溃", "想哭", "绝望"]


@app.post("/api/v1/moderate/scan")
def moderate_scan(req: ModerationReq):
    """内容风控扫描：给社区/打卡文本定级。

    返回级别语义（与 Java 侧约定）：
      ALERT  危机信号      → 内容进待审核，并给用户生成红色风险工单
      BLOCK  违规导流      → 内容进待审核（不生成心理工单）
      WATCH  消极情绪      → 内容正常发布，但生成黄色预警工单（消极人选筛查）
      NONE   干净          → 正常发布
    """
    text = (req.text or "").strip()
    if not text:
        return {"level": "NONE", "score": 0, "tags": [], "action": "PUBLISH", "model": "rules-moderate-v1"}

    crisis = [w for w in CRISIS_WORDS if w in text]
    if crisis:
        return {"level": "ALERT", "score": 100, "tags": crisis[:5],
                "action": "REVIEW_AND_ALERT", "model": "rules-moderate-v1"}

    blocked = [w for w in BLOCK_WORDS if w.lower() in text.lower()]
    if blocked:
        return {"level": "BLOCK", "score": 90, "tags": blocked[:5],
                "action": "REVIEW", "model": "rules-moderate-v1"}

    neg = [w for w in NEGATIVE_WORDS if w in text]
    if len(neg) >= 2:
        return {"level": "WATCH", "score": min(60 + len(neg) * 5, 89), "tags": neg[:5],
                "action": "PUBLISH_AND_ALERT", "model": "rules-moderate-v1"}

    return {"level": "NONE", "score": 0, "tags": [], "action": "PUBLISH", "model": "rules-moderate-v1"}


@app.get("/health")
def health():
    return {"status": "ok", "model": "rules-v0"}


@app.post("/api/v1/consult/reply")
def consult_reply(req: ConsultReq):
    """私密咨询的开场共情文案。

    刻意只做“接住情绪 + 邀请补充信息”，医学结论由后端审核过的知识卡给出，
    避免模型在缺乏面诊信息时输出诊断或用药建议。
    """
    text = (req.message or "").strip()
    if not text:
        return {"reply": "", "model": "rules-v0"}

    opening = "谢谢你愿意说这些。身体的困扰不需要羞耻，能开口已经很不容易。"
    if req.topic:
        opening += f"关于「{req.topic}」，我们先不急着下结论。"

    if any(w in text for w in ["疼", "痛", "出血", "发烧", "发热"]):
        follow = "你提到的这些感受需要被认真对待。为了判断下一步，我想再确认：持续多久了？有没有发热、明显疼痛加重或异常出血？"
    elif any(w in text for w in ["痒", "异味", "分泌物"]):
        follow = "这类情况很常见，先不用自责。可以再补充：是第一次出现还是反复？有没有伴随瘙痒、疼痛或异味？"
    elif any(w in text for w in ["月经", "周期", "推迟", "量少", "量多"]):
        follow = "周期波动受压力、体重、睡眠影响很大。最近三个月的开始日期还记得吗？有没有停经超过三个月？"
    else:
        follow = "可以再补充三点：持续多久了、有没有伴随疼痛或发热、最近有没有换洗液/用药/作息变化。"

    return {
        "reply": f"{opening}\n{follow}",
        "model": "rules-v0",
        "disclaimer": "本回复为健康教育与就诊准备信息，不构成诊断或用药建议",
    }


@app.post("/api/v1/emotion/predict")
def predict(req: EmotionReq):
    contributions = []

    # 1. 危机关键词 → 直接红档（最高优先级，不可被其他信号覆盖）
    crisis_hit = [w for w in CRISIS_WORDS if w in req.text]
    if crisis_hit:
        return {
            "risk_prob": 1.0,
            "level": "红",
            "contributions": [{"feature": f"危机关键词:{crisis_hit[0]}", "weight": 1.0}],
            "action": "立即展示求助入口与热线，生成红档工单，通知人工跟进",
            "model": "rules-v0",
        }

    # 2. 量表分数（EPDS >= 10 → 黄）
    epds = int(req.scale_scores.get("epds", 0))
    if epds >= 10:
        contributions.append({"feature": "EPDS得分", "weight": round(epds / 30, 2)})

    # 3. 文本负面词
    neg_hits = [w for w in NEGATIVE_WORDS if w in req.text]
    if neg_hits:
        contributions.append({"feature": f"负面表达:{neg_hits[0]}", "weight": 0.2})

    # 4. 行为信号（连续低落天数）
    low_days = int(req.behavior_features.get("consecutive_low_days", 0))
    if low_days >= 5:
        contributions.append({"feature": f"连续低落{low_days}天", "weight": 0.25})

    yellow = epds >= 10 or low_days >= 5 or len(neg_hits) >= 2
    level = "黄" if yellow else "绿"
    risk_prob = min(sum(c["weight"] for c in contributions), 0.99)

    return {
        "risk_prob": round(risk_prob, 2),
        "level": level,
        "contributions": contributions,
        "action": "生成黄档工单，48小时内人工联系" if yellow else "推荐自助内容",
        "model": "rules-v0",
        "disclaimer": "本结果为风险筛查参考，不构成任何医学诊断",
    }
