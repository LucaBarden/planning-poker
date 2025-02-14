#!/bin/bash

# Find the last tag; if none exists, use all commits.
LAST_TAG=$(git describe --tags --abbrev=0 2>/dev/null || echo "")
if [ -z "$LAST_TAG" ]; then
  echo "No previous tag found, using all commits" >&2
  CHANGES=$(git log --pretty=format:"* %s")
else
  echo "Found last tag: $LAST_TAG" >&2
  CHANGES=$(git log ${LAST_TAG}..HEAD --pretty=format:"* %s")
fi

echo "Commit messages since last release:" >&2
echo "$CHANGES" >&2

# Prepare the user prompt without role markers (roles are defined in the messages array)
USER_PROMPT="Generate release notes in Markdown format that include:
🔥 **What's New?** – Summarize the new features and improvements.
🚀 **Upcoming Features** – Mention any features on the horizon.
💡 **Get Involved!** – Encourage community participation.
Use the following commit messages as context:
$CHANGES
"

echo "User Prompt:" >&2
echo "$USER_PROMPT" >&2

# Build the JSON payload using jq with the new messages API structure
DATA=$(jq -n --arg user_message "$USER_PROMPT" '{
  messages: [
    {role: "user", content: $user_message}
  ],
  model: "claude-3-5-sonnet-20241022",
  system: "You are a release note generator.",
  max_tokens: 2048
}')

echo "JSON Payload:" >&2
echo "$DATA" >&2

# Call the Anthropic API.
RESPONSE=$(curl -s -X POST https://api.anthropic.com/v1/messages \
  -H "Content-Type: application/json" \
  -H "anthropic-version: 2023-06-01" \
  -H "x-api-key: ${ANTHROPIC_API_KEY}" \
  -d "$DATA")

echo "Full API Response:" >&2
echo "$RESPONSE" | jq . >&2

# Extract the generated release notes from the "completion" field.
RELEASE_BODY=$(echo "$RESPONSE" | jq -r '.content[0].text')

if [ "$RELEASE_BODY" = "null" ] || [ -z "$RELEASE_BODY" ]; then
  echo "Error: Release body not generated. Please check the API response above." >&2
fi

echo "Generated Release Body:" >&2
echo "$RELEASE_BODY"

