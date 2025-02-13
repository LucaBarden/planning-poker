#!/bin/bash
set -e

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

# Prepare the prompt for ChatGPT.
PROMPT="Generate release notes in Markdown format that include:
🔥 **What's New?** – Summarize the new features and improvements.
🚀 **Upcoming Features** – Mention any features on the horizon.
💡 **Get Involved!** – Encourage community participation.
Use the following commit messages as context:
$CHANGES
"

echo "Prompt for ChatGPT:" >&2
echo "$PROMPT" >&2

# Call the OpenAI API (using the gpt-4o-mini model)
RESPONSE=$(curl -s -X POST https://api.openai.com/v1/chat/completions \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer ${OPENAI_API_KEY}" \
  -d "{
        \"model\": \"gpt-4o-mini\",
        \"messages\": [
          {\"role\": \"system\", \"content\": \"You are a release note generator.\"},
          {\"role\": \"user\", \"content\": \"$PROMPT\"}
        ]
      }")

# Extract the release notes using jq.
RELEASE_BODY=$(echo "$RESPONSE" | jq -r '.choices[0].message.content')
echo "Generated Release Body:" >&2
echo "$RELEASE_BODY" >&2

# Output the release body to stdout.
echo "$RELEASE_BODY"
