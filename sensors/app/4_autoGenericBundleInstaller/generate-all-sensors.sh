#!/bin/bash

echo "==================="
echo "Generating all sensors"
echo "==================="

declare -a scripts=(
  "trafficSensGen.sh"
  "bandSensGen.sh"
  "latency-sens.sh"
  "packetloss-sens.sh"
  "suspiciousactivity.sh"
)

for script in "${scripts[@]}"; do
  if [[ -x "$script" ]]; then
    echo "▶ Running $script ..."
    ./"$script"
    echo "✅ Done: $script"
    echo ""
  else
    echo "❌ Script not found or not executable: $script"
  fi
done

echo "==================="
echo "All sensor jars processed."
