---
name: git-issue-creation
description: Handles the creation of issues on GitHub or GitLab via MCP using repository issue templates (or proposing to create them if absent), either autonomously or upon detecting a missing ticket number for a branch, subject to user validation before remote writing.
---

# Git Issue Creation

Use this skill to create an issue on GitHub or GitLab while respecting the repository's templates, either upon the
user's explicit request or when a ticket number is missing during branch creation. Explicit user validation is required
before any remote write action.

## Instructions

1. **Trigger:**
    - Upon the user's explicit request to create an issue.
    - Or if the user requests a branch creation (`git-branch-creation`) without specifying a valid ticket number.

2. **Check and Adapt Repository Templates:**
    - Inspect the repository to locate existing issue templates (e.g., under `.github/ISSUE_TEMPLATE/` or equivalent
      GitLab paths).
    - **If no templates are present:** Ask the user if they wish to create suitable templates for standard types (e.g.,
      `bug.md`, `feature.md`, `debt.md`, etc.) before proceeding to draft the issue.
    - If templates exist, select the one matching the requirement and ask the user for the necessary details.

3. **User Validation Before Remote Action:**
    - Present the drafted issue title, template type, and body to the user.
    - **Mandatory:** Wait for explicit user validation/approval before performing any remote write or creation action.
    - Remote writes through the CLI are denied by `.claude/settings.json`; if the MCP creation is unavailable, hand the
      drafted text to the user instead.

4. **MCP Integration:**
    - Once validated by the user, use available MCP tools to create the issue on the corresponding platform (GitHub or
      GitLab).
    - Retrieve and communicate the generated issue number/ID.
    - If the initial context was a branch creation, use this new number to name the branch
      (`<username_git>/<type>/<ticket_number>`).
