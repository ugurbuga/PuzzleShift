#!/bin/sh
set -eu

SCRIPT_DIR="$(CDPATH= cd -- "$(dirname "$0")" && pwd)"
ROOT_DIR="$(CDPATH= cd -- "$SCRIPT_DIR/../.." && pwd)"
TARGET_FILE="$SCRIPT_DIR/GeneratedAds.xcconfig"

SOURCE_FILE=""
for candidate in \
  "$ROOT_DIR/iosApp/iosApp/ads.properties" \
  "$ROOT_DIR/ads.properties"
do
  if [ -f "$candidate" ]; then
    SOURCE_FILE="$candidate"
    break
  fi
done

if [ -z "$SOURCE_FILE" ]; then
  rm -f "$TARGET_FILE"
  exit 0
fi

read_property() {
  key="$1"
  file="$2"
  awk -F '=' -v key="$key" '
    $0 ~ /^[[:space:]]*#/ { next }
    index($0, "=") == 0 { next }
    {
      currentKey = $1
      sub(/^[[:space:]]+/, "", currentKey)
      sub(/[[:space:]]+$/, "", currentKey)
      if (currentKey != key) next
      value = substr($0, index($0, "=") + 1)
      sub(/^[[:space:]]+/, "", value)
      sub(/[[:space:]]+$/, "", value)
      print value
      exit
    }
  ' "$file"
}

application_id="$(read_property "ads.ios.stackshift.applicationId" "$SOURCE_FILE")"
if [ -z "$application_id" ]; then
  application_id="$(read_property "ads.ios.applicationId" "$SOURCE_FILE")"
fi

if [ -z "$application_id" ]; then
  rm -f "$TARGET_FILE"
  exit 0
fi

cat > "$TARGET_FILE" <<EOF
ADS_IOS_APPLICATION_ID=$application_id
EOF

