# Skullking

## Install dev env
You must generate your `service-account-file.json` in firebase, and configure environment variables in `dev.yml` with valid values.
Regenerate another `service-account-file.json` to expire the previous one (that is used only for `test` and `dev`)
Use the latest generated to configure `env vars` according on `config.yml` in you remote deployment utility.

```
npm install -g firebase-tools
```