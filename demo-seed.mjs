// 演示数据脚本：创建一个带数据的演示账号（幂等，可重复运行）
// 用法：node demo-seed.mjs            （后端需已在 8080 运行）
// 账号：13800000001 / demo123456      孕期约 28 周
const base = process.env.API_BASE || 'http://localhost:8080';
const phone = '13800000001';
const password = 'demo123456';

const json = (o) => JSON.stringify(o);
const today = new Date();

async function call(method, path, body, token) {
  const res = await fetch(base + path, {
    method,
    headers: {
      'Content-Type': 'application/json',
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
    },
    body: body ? json(body) : undefined,
  });
  return res.json();
}

const daysAgo = (n) => {
  const d = new Date(today);
  d.setDate(d.getDate() - n);
  // 后端用 LocalDateTime，这里给本地时间字符串（不带 Z）
  const p = (x) => String(x).padStart(2, '0');
  return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())}T${p(d.getHours())}:${p(d.getMinutes())}:00`;
};

const ymd = (d) => {
  const p = (x) => String(x).padStart(2, '0');
  return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())}`;
};

const lmp = new Date(today);
lmp.setDate(lmp.getDate() - 196); // 28 周

async function main() {
  let token;
  const reg = await call('POST', '/api/auth/register', { phone, password, nickname: '小满' });
  if (reg.code === 0) {
    token = reg.data.token;
    console.log('已注册演示账号', phone);
  } else {
    const login = await call('POST', '/api/auth/login', { phone, password });
    if (login.code !== 0) throw new Error('登录失败：' + login.msg);
    token = login.data.token;
    console.log('已存在，直接登录', phone);
  }

  const mine = await call('GET', '/api/profiles/mine', null, token);
  if (mine.code === 40401) {
    const created = await call('POST', '/api/profiles', {
      lmpDate: ymd(lmp),
      nickname: '小满',
      concerns: ['checkup', 'weight', 'mood'],
    }, token);
    if (created.code !== 0) throw new Error('建档失败：' + created.msg);
    console.log(`已建档：孕 ${created.data.weeks}+${created.data.days} 周，预产期 ${created.data.edd}`);
  } else {
    console.log(`已有档案：孕 ${mine.data.weeks}+${mine.data.days} 周`);
  }

  // 幂等：已有数据就不再重复灌，保证脚本可以反复运行
  async function ensure(type, target, build) {
    const list = await call(`GET`, `/api/health-records?type=${type}`, null, token);
    const have = (list.data || []).length;
    if (have >= target) {
      console.log(`${type} 已有 ${have} 条，跳过`);
      return;
    }
    for (const body of build) {
      const r = await call('POST', '/api/health-records', { type, ...body }, token);
      if (r.code !== 0) console.log(`${type} 写入失败：`, r.msg);
    }
    console.log(`${type} 已补 ${build.length} 条`);
  }

  const weights = [58.6, 59.0, 59.6, 60.1, 60.4, 60.8, 61.2, 61.7];
  await ensure('WEIGHT', weights.length, weights.map((v, i) => ({
    value: v,
    measuredAt: daysAgo((weights.length - 1 - i) * 4),
  })));

  const bps = [[112, 72], [118, 76], [121, 79]];
  await ensure('BLOOD_PRESSURE', bps.length, bps.map(([s, d], i) => ({
    value: s,
    value2: d,
    measuredAt: daysAgo((bps.length - 1 - i) * 7),
  })));

  const glucoses = [4.6, 4.9];
  await ensure('BLOOD_GLUCOSE', glucoses.length, glucoses.map((v, i) => ({
    value: v,
    measuredAt: daysAgo((glucoses.length - 1 - i) * 10),
    note: '空腹',
  })));

  const fetal = [5, 7];
  await ensure('FETAL_MOVEMENT', fetal.length, fetal.map((v, i) => ({
    value: v,
    measuredAt: daysAgo((fetal.length - 1 - i) * 3),
    note: '晚饭后一小时',
  })));

  const checkups = await call('GET', '/api/checkups', null, token);
  const planned = (checkups.data || []).filter((c) => c.status === 'PLANNED');
  if (planned.length) {
    const done = planned.slice(0, 2);
    for (const c of done) {
      await call('PUT', `/api/checkups/${c.id}`, { status: 'DONE' }, token);
    }
    console.log(`已标记 ${done.length} 个产检项为已完成（共 ${checkups.data.length} 项）`);
  }

  // ---------- W4：家属协同演示数据 ----------
  const famMine = await call('GET', '/api/family/mine', null, token);
  let inviteCode = null;
  if (famMine.code === 0 && famMine.data.role === 'NONE') {
    const created = await call('POST', '/api/family/invitations', null, token);
    if (created.code === 0) {
      inviteCode = created.data.code;
      console.log('已创建家庭，邀请码 ' + inviteCode);
    }
  }

  const memberPhone = '13800000002';
  const memberPwd = 'demo123456';
  let memberToken;
  const memberReg = await call('POST', '/api/auth/register', {
    phone: memberPhone, password: memberPwd, nickname: '阿哲',
  });
  if (memberReg.code === 0) {
    memberToken = memberReg.data.token;
    console.log('已注册家属账号 ' + memberPhone);
  } else {
    const l = await call('POST', '/api/auth/login', { phone: memberPhone, password: memberPwd });
    if (l.code !== 0) throw new Error('家属账号登录失败：' + l.msg);
    memberToken = l.data.token;
    console.log('家属账号已存在，直接登录');
  }

  const memberMine = await call('GET', '/api/family/mine', null, memberToken);
  if (memberMine.code === 0 && memberMine.data.role === 'NONE') {
    if (!inviteCode) {
      const created = await call('POST', '/api/family/invitations', null, token);
      inviteCode = created.code === 0 ? created.data.code : null;
    }
    const joined = await call('POST', `/api/family/join/${inviteCode}`, null, memberToken);
    console.log(joined.code === 0 ? '家属已加入家庭' : '家属加入失败：' + joined.msg);
  } else {
    console.log('家属已在家庭中，跳过加入');
  }

  const members = await call('GET', '/api/family/members', null, token);
  const first = (members.data || [])[0];
  if (first) {
    const perm = await call('PUT', `/api/family/members/${first.id}/permissions`, {
      visibleFields: ['WEIGHT', 'BLOOD_PRESSURE'],
      relation: '配偶',
    }, token);
    if (perm.code !== 0) console.log('授权失败：' + perm.msg);
    else console.log('已授权家属查看：体重 / 血压（血糖、胎动、情绪仍不可见）');
  }

  const momTasks = await call('GET', '/api/tasks', null, token);
  if ((momTasks.data || []).length === 0 && first) {
    const assigned = await call('POST', '/api/tasks/assign', {
      templateCode: 'LATE_PREGNANCY',
      assigneeUserId: first.userId,
    }, token);
    console.log(assigned.code === 0
      ? `已下发任务包「孕晚期待产准备」：${assigned.data.length} 项`
      : '下发失败：' + assigned.msg);
  }

  const myTasks = await call('GET', '/api/tasks', null, memberToken);
  const pending = (myTasks.data || []).find((t) => t.status === 'PENDING');
  if (pending) {
    await call('PUT', `/api/tasks/${pending.id}/status`, { status: 'DOING' }, memberToken);
    console.log(`家属把「${pending.title}」标记为进行中`);
  }

  console.log('\n演示账号就绪：');
  console.log(`  妈妈：${phone} / ${password}  （孕 28 周，有指标与报告）`);
  console.log(`  家属：${memberPhone} / ${memberPwd}  （已绑定，可见体重/血压 + 任务）`);
}

main().catch((e) => {
  console.error('FAILED:', e.message);
  process.exit(1);
});
