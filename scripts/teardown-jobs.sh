#!/usr/bin/env bash
# Removes all jobs owned by the seed user created by seed-jobs.sh.
# Requires: curl, jq. Assumes the API is running at $API_BASE (default localhost:8080).
#
# Usage:
#   ./scripts/teardown-jobs.sh
#   API_BASE=http://staging.jobboard.local ./scripts/teardown-jobs.sh

set -euo pipefail

API_BASE="${API_BASE:-http://localhost:8080}"
SEED_EMAIL="${SEED_EMAIL:-seed@jobboard.local}"
SEED_PASSWORD="${SEED_PASSWORD:-seedpass123}"

command -v jq >/dev/null || { echo "jq is required"; exit 1; }

echo "→ Logging in as $SEED_EMAIL ..."
login_resp=$(curl -sS -w "\n%{http_code}" -X POST "$API_BASE/api/auth/login" \
    -H 'Content-Type: application/json' \
    -d "{\"email\":\"$SEED_EMAIL\",\"password\":\"$SEED_PASSWORD\"}")
login_body=$(echo "$login_resp" | sed '$d')
login_code=$(echo "$login_resp" | tail -n1)

if [ "$login_code" != "200" ]; then
  echo "Login failed ($login_code): $login_body"
  echo "Nothing to tear down — seed user does not exist."
  exit 0
fi
TOKEN=$(echo "$login_body" | jq -r .token)
[ -n "$TOKEN" ] && [ "$TOKEN" != "null" ] || { echo "No token acquired"; exit 1; }
echo "✓ Auth token acquired"

echo "→ Fetching seed user's jobs from /api/users/me/jobs ..."
jobs_json=$(curl -sS "$API_BASE/api/users/me/jobs" -H "Authorization: Bearer $TOKEN")
count=$(echo "$jobs_json" | jq 'length')
if [ "$count" -eq 0 ]; then
  echo "✓ No jobs to delete."
  exit 0
fi
echo "→ Found $count job(s) to delete."

i=0
while read -r row; do
  i=$((i + 1))
  job_id=$(echo "$row" | jq -r .id)
  title=$(echo "$row" | jq -r .title)
  location=$(echo "$row" | jq -r .location)

  http_code=$(curl -sS -o /dev/null -w "%{http_code}" \
      -X DELETE "$API_BASE/api/jobs/$job_id" \
      -H "Authorization: Bearer $TOKEN")
  if [ "$http_code" = "204" ] || [ "$http_code" = "200" ]; then
    printf "  ✓ [%2d] deleted #%s  %-30s  %s\n" "$i" "$job_id" "$title" "$location"
  else
    printf "  ✗ [%2d] failed (%s) #%s  %s\n" "$i" "$http_code" "$job_id" "$title"
  fi
done < <(echo "$jobs_json" | jq -c '.[]')

echo "✓ Done. Removed jobs for seed user."
