# Project handoff

- Communicate with the user in Bengali (বাংলা).
- Before working on this repository, read `WORK_IN_PROGRESS.md`. It records the accepted premium UI plan, implementation checkpoint, failed build and exact next steps.
- This is unfinished work. Do not assume builds/tests passed or restart the redesign from scratch.
- Use the actual checkout path. The old `/workspace/clever-lovelace` path in GEMINI.md is stale for this Termux environment.
- Build native Android through the available Android Development environment (`adev`); preserve the pinned project dependencies. Details are in the checkpoint.
- Never uninstall the existing ERP app to resolve an APK signing mismatch, clear user data, or run test restores against the user's live database.
- Do not spawn sub-agents unless the user explicitly requests delegation.
