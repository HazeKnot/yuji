# Yuji W1-W2 smoke test: register -> no-profile check -> create profile -> checkups -> mark done
# ASCII only (Windows PowerShell 5.1 reads BOM-less files as ANSI)
# Idempotent: uses a random phone per run
$base = 'http://localhost:8080'
$ErrorActionPreference = 'Stop'

function Json($o) { $o | ConvertTo-Json -Compress }

# 0. health
$health = Invoke-RestMethod -Uri "$base/actuator/health"
Write-Host "[0] health = $($health.status)"

# 1. register (random phone)
$phone = '13' + (Get-Random -Minimum 100000000 -Maximum 999999999)
$reg = Invoke-RestMethod -Method Post -Uri "$base/api/auth/register" -ContentType 'application/json' -Body (Json @{ phone = $phone; password = 'test123456'; nickname = 'XiaoLin' })
Write-Host "[1] register -> phone=$phone code=$($reg.code), userId=$($reg.data.user.id)"
$token = $reg.data.token
if (-not $token) { Write-Host 'FAIL: no token'; exit 1 }
$headers = @{ Authorization = "Bearer $token" }

# 2. no-profile check (expect code 40401 with HTTP 200)
$mine = Invoke-RestMethod -Uri "$base/api/profiles/mine" -Headers $headers
if ($mine.code -eq 40401) {
  Write-Host "[2] no-profile check -> code=$($mine.code)  [OK]"
} else {
  Write-Host "[2] FAIL: expect 40401, got $($mine.code)"; exit 1
}

# 3. create profile (lmp = 196 days ago => week 28)
$lmp = (Get-Date).AddDays(-196).ToString('yyyy-MM-dd')
$p = Invoke-RestMethod -Method Post -Uri "$base/api/profiles" -Headers $headers -ContentType 'application/json' -Body (Json @{ lmpDate = $lmp; nickname = 'XiaoLin'; concerns = @('checkup', 'mood') })
Write-Host "[3] profile -> week $($p.data.weeks)+$($p.data.days), stage=$($p.data.stage), daysToEdd=$($p.data.daysToEdd), edd=$($p.data.edd)"
if ($p.data.weeks -ne 28) { Write-Host "FAIL: expect week 28, got $($p.data.weeks)"; exit 1 }

# 4. checkups (expect >= 10 items auto-generated)
$c = Invoke-RestMethod -Uri "$base/api/checkups" -Headers $headers
Write-Host "[4] checkups -> total $($c.data.Count)"
if ($c.data.Count -lt 10) { Write-Host 'FAIL: expect >= 10 checkup items'; exit 1 }

# 5. mark first planned item DONE
$first = $c.data | Where-Object { $_.status -eq 'PLANNED' } | Select-Object -First 1
$u = Invoke-RestMethod -Method Put -Uri "$base/api/checkups/$($first.id)" -Headers $headers -ContentType 'application/json' -Body (Json @{ status = 'DONE' })
Write-Host "[5] mark done -> id=$($u.data.id) name=$($u.data.name) status=$($u.data.status)"
if ($u.data.status -ne 'DONE') { Write-Host "FAIL: status not updated, resp code=$($u.code) msg=$($u.msg)"; exit 1 }

# 6. invalid status should be rejected with 40000
$bad = Invoke-RestMethod -Method Put -Uri "$base/api/checkups/$($first.id)" -Headers $headers -ContentType 'application/json' -Body (Json @{ status = 'WHATEVER' })
if ($bad.code -eq 40000) {
  Write-Host '[6] invalid status rejected  [OK]'
} else {
  Write-Host "[6] FAIL: expect 40000, got $($bad.code)"; exit 1
}

# 7. health record: weight + blood pressure (cross-check rejected)
$r1 = Invoke-RestMethod -Method Post -Uri "$base/api/health-records" -Headers $headers -ContentType 'application/json' -Body (Json @{ type = 'WEIGHT'; value = 61.5 })
Write-Host "[7] record weight -> $($r1.data.value)$($r1.data.unit) code=$($r1.code)"
if ($r1.code -ne 0) { Write-Host 'FAIL: record weight'; exit 1 }

$bp = Invoke-RestMethod -Method Post -Uri "$base/api/health-records" -Headers $headers -ContentType 'application/json' -Body (Json @{ type = 'BLOOD_PRESSURE'; value = 118; value2 = 76 })
if ($bp.code -ne 0) { Write-Host 'FAIL: record blood pressure'; exit 1 }
$badBp = Invoke-RestMethod -Method Post -Uri "$base/api/health-records" -Headers $headers -ContentType 'application/json' -Body (Json @{ type = 'BLOOD_PRESSURE'; value = 80; value2 = 120 })
if ($badBp.code -eq 40000) {
  Write-Host '[8] inverted blood pressure rejected  [OK]'
} else {
  Write-Host "[8] FAIL: expect 40000, got $($badBp.code)"; exit 1
}

# 9. trend (expect at least one point, with disclaimer)
$t = Invoke-RestMethod -Uri "$base/api/health-records/trend?type=WEIGHT&days=30" -Headers $headers
Write-Host "[9] trend -> points=$($t.data.points.Count) ref=$($t.data.refMin)-$($t.data.refMax) disclaimer=$($t.data.disclaimer.Length) chars"
if ($t.data.points.Count -lt 1) { Write-Host 'FAIL: trend has no points'; exit 1 }

# 10. report upload (multipart via curl.exe) + list + download + delete
$tmp = Join-Path $env:TEMP ('yuji-report-' + (Get-Random) + '.pdf')
[System.IO.File]::WriteAllText($tmp, '%PDF-1.4 yuji smoke report')
$up = curl.exe -s -X POST "$base/api/checkups/$($first.id)/reports" -H "Authorization: Bearer $token" -F "file=@$tmp;type=application/pdf" | ConvertFrom-Json
Write-Host "[10] upload report -> code=$($up.code) name=$($up.data.fileName) size=$($up.data.size)$($up.data.downloadUrl)"
if ($up.code -ne 0) { Write-Host 'FAIL: upload report'; exit 1 }

$list = Invoke-RestMethod -Uri "$base/api/checkups/$($first.id)/reports" -Headers $headers
if ($list.data.Count -lt 1) { Write-Host 'FAIL: report list empty'; exit 1 }

$out = Join-Path $env:TEMP ('yuji-dl-' + (Get-Random) + '.pdf')
$code = curl.exe -s -o $out -w "%{http_code}" -H "Authorization: Bearer $token" "$base/api/reports/$($up.data.id)/download"
$size = (Get-Item $out).Length
Write-Host "[11] download report -> http=$code bytes=$size"
if ($code -ne '200' -or $size -le 0) { Write-Host 'FAIL: download report'; exit 1 }

$del = Invoke-RestMethod -Method Delete -Uri "$base/api/reports/$($up.data.id)" -Headers $headers
$after = Invoke-RestMethod -Uri "$base/api/checkups/$($first.id)/reports" -Headers $headers
Write-Host "[12] delete report -> code=$($del.code), remaining=$($after.data.Count)"
if ($after.data.Count -ne 0) { Write-Host 'FAIL: report not deleted'; exit 1 }

Remove-Item $tmp, $out -ErrorAction SilentlyContinue

# ================= W4: family collaboration =================

# 13. mom generates an invitation code
$inv = Invoke-RestMethod -Method Post -Uri "$base/api/family/invitations" -Headers $headers
Write-Host "[13] invitation -> code=$($inv.data.code) expires=$($inv.data.expiresAt)"
if ($inv.code -ne 0 -or -not $inv.data.code) { Write-Host 'FAIL: create invitation'; exit 1 }

# 14. family member registers (a fresh phone), then joins with the code
$phone2 = '13' + (Get-Random -Minimum 100000000 -Maximum 999999999)
$reg2 = Invoke-RestMethod -Method Post -Uri "$base/api/auth/register" -ContentType 'application/json' -Body (Json @{ phone = $phone2; password = 'test123456'; nickname = 'A-Zhe' })
$token2 = $reg2.data.token
$familyUserId = $reg2.data.user.id
$headers2 = @{ Authorization = "Bearer $token2" }

$join = Invoke-RestMethod -Method Post -Uri "$base/api/family/join/$($inv.data.code)" -Headers $headers2
Write-Host "[14] join -> role=$($join.data.role) owner=$($join.data.ownerUserId) members=$($join.data.memberCount)"
if ($join.data.role -ne 'MEMBER') { Write-Host "FAIL: join failed code=$($join.code) msg=$($join.msg)"; exit 1 }

# 15. by default nothing is shared: reading mom's metrics must be rejected (40300)
$denied = Invoke-RestMethod -Uri "$base/api/family/shared/records?type=WEIGHT" -Headers $headers2
if ($denied.code -eq 40300) {
  Write-Host '[15] metric not shared yet -> 40300  [OK]'
} else {
  Write-Host "[15] FAIL: expect 40300, got $($denied.code)"; exit 1
}

# 16. but the status summary is visible and carries the privacy note
$sum = Invoke-RestMethod -Uri "$base/api/family/shared/summary" -Headers $headers2
Write-Host "[16] summary -> weeks=$($sum.data.weeks)+$($sum.data.days) checkup=$($sum.data.checkupDone)/$($sum.data.checkupTotal) note=$($sum.data.privacyNote.Length) chars"
if ($sum.code -ne 0) { Write-Host 'FAIL: summary'; exit 1 }

# 17. mom grants WEIGHT only
$members = Invoke-RestMethod -Uri "$base/api/family/members" -Headers $headers
$memberRec = $members.data | Where-Object { $_.userId -eq $familyUserId } | Select-Object -First 1
$perm = Invoke-RestMethod -Method Put -Uri "$base/api/family/members/$($memberRec.id)/permissions" -Headers $headers -ContentType 'application/json' -Body (Json @{ visibleFields = @('WEIGHT'); relation = 'Spouse' })
Write-Host "[17] grant WEIGHT -> $($perm.data.visibleFields -join ',')"
if ($perm.code -ne 0) { Write-Host "FAIL: grant: $($perm.msg)"; exit 1 }

$visible = Invoke-RestMethod -Uri "$base/api/family/shared/records?type=WEIGHT" -Headers $headers2
Write-Host "[18] family reads shared weight -> rows=$($visible.data.Count)"
if ($visible.code -ne 0 -or $visible.data.Count -lt 1) { Write-Host 'FAIL: shared records'; exit 1 }

# 19. still cannot read a non-authorized metric (blood pressure)
$denied2 = Invoke-RestMethod -Uri "$base/api/family/shared/records?type=BLOOD_PRESSURE" -Headers $headers2
if ($denied2.code -eq 40300) {
  Write-Host '[19] unauthorized metric still 40300  [OK]'
} else {
  Write-Host "[19] FAIL: expect 40300, got $($denied2.code)"; exit 1
}

# 20. family member cannot change permissions (owner only)
$escalate = Invoke-RestMethod -Method Put -Uri "$base/api/family/members/$($memberRec.id)/permissions" -Headers $headers2 -ContentType 'application/json' -Body (Json @{ visibleFields = @('WEIGHT', 'BLOOD_PRESSURE') })
if ($escalate.code -eq 40300) {
  Write-Host '[20] member cannot self-grant -> 40300  [OK]'
} else {
  Write-Host "[20] FAIL: expect 40300, got $($escalate.code)"; exit 1
}

# 21. mom assigns a task package to the family member
$tpls = Invoke-RestMethod -Uri "$base/api/task-templates" -Headers $headers
Write-Host "[21] templates -> $($tpls.data.Count) packs, first=$($tpls.data[0].code) items=$($tpls.data[0].items.Count)"
$assign = Invoke-RestMethod -Method Post -Uri "$base/api/tasks/assign" -Headers $headers -ContentType 'application/json' -Body (Json @{ templateCode = 'POSTPARTUM_W1'; assigneeUserId = $familyUserId })
Write-Host "[22] assign -> created $($assign.data.Count) tasks"
if ($assign.code -ne 0 -or $assign.data.Count -lt 1) { Write-Host 'FAIL: assign'; exit 1 }

$myTasks = Invoke-RestMethod -Uri "$base/api/tasks" -Headers $headers2
$mineTrue = @($myTasks.data | Where-Object { $_.mine }).Count
Write-Host "[23] family sees own tasks -> total=$($myTasks.data.Count) mine=$mineTrue"
if ($myTasks.data.Count -lt 1 -or $mineTrue -lt 1) { Write-Host 'FAIL: task list scope'; exit 1 }

# 24. family member checks in, mom sees the progress immediately
$firstTask = $myTasks.data[0]
$done = Invoke-RestMethod -Method Put -Uri "$base/api/tasks/$($firstTask.id)/status" -Headers $headers2 -ContentType 'application/json' -Body (Json @{ status = 'DONE' })
if ($done.data.status -ne 'DONE' -or -not $done.data.completedAt) { Write-Host 'FAIL: check-in'; exit 1 }

$momTasks = Invoke-RestMethod -Uri "$base/api/tasks" -Headers $headers
# 注意：管道输出单个对象时 .Count 为空，必须用 @() 强制成数组
$doneCount = @($momTasks.data | Where-Object { $_.status -eq 'DONE' }).Count
Write-Host "[24] check-in -> mom sees done=$doneCount (assignee=$($done.data.assigneeNickname))"
if ($doneCount -lt 1) { Write-Host 'FAIL: mom cannot see progress'; exit 1 }

# 25. one-time code: reusing the same invitation must fail
$again = Invoke-RestMethod -Method Post -Uri "$base/api/family/join/$($inv.data.code)" -Headers $headers2
Write-Host "[25] reuse invitation -> code=$($again.code)"
if ($again.code -eq 0) { Write-Host 'FAIL: invitation should be single-use'; exit 1 }

# ================= W5: shared trend + org dashboard =================

# 26. family member reads the authorized metric trend (weight was granted earlier)
$trend = Invoke-RestMethod -Uri "$base/api/family/shared/trend?type=WEIGHT&days=30" -Headers $headers2
Write-Host "[26] shared trend -> points=$($trend.data.points.Count) unit=$($trend.data.unit)"
if ($trend.code -ne 0 -or $trend.data.points.Count -lt 1) { Write-Host 'FAIL: shared trend'; exit 1 }

# 27. org account login (seeded by V9) and read the dashboard
$org = Invoke-RestMethod -Method Post -Uri "$base/api/auth/login" -ContentType 'application/json' -Body (Json @{ phone = 'org001'; password = 'org123456' })
if ($org.code -ne 0 -or $org.data.user.role -ne 'ORG') { Write-Host "FAIL: org login code=$($org.code) msg=$($org.msg)"; exit 1 }
$orgHeaders = @{ Authorization = "Bearer $($org.data.token)" }

$dash = Invoke-RestMethod -Uri "$base/api/admin/dashboard" -Headers $orgHeaders
$buckets = @($dash.data.weekBuckets).Count
$recent = @($dash.data.recentRecords).Count
Write-Host "[27] org dashboard -> moms=$($dash.data.momCount) records=$($dash.data.recordCount) taskRate=$($dash.data.taskRate)% buckets=$buckets recent=$recent"
if ($dash.code -ne 0 -or $dash.data.momCount -lt 1 -or $buckets -ne 4 -or $recent -ne 7) { Write-Host 'FAIL: dashboard shape'; exit 1 }

# 28. a mom account must not read the org dashboard
$deniedDash = Invoke-RestMethod -Uri "$base/api/admin/dashboard" -Headers $headers
if ($deniedDash.code -eq 40300) {
  Write-Host '[28] mom cannot read org dashboard -> 40300  [OK]'
} else {
  Write-Host "[28] FAIL: expect 40300, got $($deniedDash.code)"; exit 1
}

# ================= W5: knowledge pushed by gestational week =================

# 29. home "this week" knowledge: recommended from the current week
$know = Invoke-RestMethod -Uri "$base/api/articles/for-me?limit=5" -Headers $headers
Write-Host "[29] knowledge for-me -> $($know.data.Count) items, first=$($know.data[0].code) $($know.data[0].weekHint)"
if ($know.code -ne 0 -or $know.data.Count -lt 1) { Write-Host 'FAIL: for-me empty'; exit 1 }

# 30. every recommended item must cover the current week (no 28-week mom getting 12-week content)
$outside = @($know.data | Where-Object { $_.minWeek -gt $p.data.weeks -or $_.maxWeek -lt $p.data.weeks }).Count
if ($outside -gt 0) { Write-Host "FAIL: $outside items outside week $($p.data.weeks)"; exit 1 }
Write-Host "[30] all $($know.data.Count) items cover week $($p.data.weeks)  [OK]"

# 31. detail carries source + disclaimer (content must be traceable)
$detail = Invoke-RestMethod -Uri "$base/api/articles/$($know.data[0].id)" -Headers $headers
Write-Host "[31] article detail -> body=$($detail.data.body.Length) chars source=$($detail.data.source)"
if ($detail.code -ne 0 -or -not $detail.data.source -or -not $detail.data.disclaimer) { Write-Host 'FAIL: detail missing source/disclaimer'; exit 1 }

# 32. unknown category is rejected instead of silently returning everything
$badCat = Invoke-RestMethod -Uri "$base/api/articles?category=SLEEP" -Headers $headers
if ($badCat.code -eq 40000) {
  Write-Host '[32] unknown category -> 40000  [OK]'
} else {
  Write-Host "[32] FAIL: expect 40000, got $($badCat.code)"; exit 1
}

Write-Host ''
Write-Host '=== SMOKE TEST PASSED ==='
