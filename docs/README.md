# Project site (GitHub Pages)

Static site served by GitHub Pages. It exists mainly to host the **privacy policy
URL** that AppGallery (and every other store) asks for during submission.

## Files

| File | Purpose |
|---|---|
| `index.html` | Landing page: features, requirements, download and support links |
| `privacy.html` | Privacy policy — this is the URL you paste into the store console |
| `privacy.ru.html` | Russian translation of the policy, matching the app's `values-ru` |
| `assets/style.css` | Shared stylesheet; teal palette mirrors the app theme |
| `assets/icon.png`, `assets/icon-216.png` | Copies of the store icons from `fastlane/metadata/huawei/images/` |
| `.nojekyll` | Serve the files as-is; skip Jekyll (the root `Gemfile` is for Fastlane, not Jekyll) |

## Enabling Pages

Repository → **Settings → Pages** → *Build and deployment*:

- **Source**: Deploy from a branch
- **Branch**: `main`, folder **`/docs`** → Save

The site goes live at `https://<owner>.github.io/<repo>/` within a minute or two.
No workflow or CI minutes are involved; every push to `main` that touches `docs/`
republishes it.

## Before publishing to a store

Search the HTML for `TODO` and fill in:

1. **Contact address** — `support@example.com` appears in `index.html` and both
   policy pages. Use an address you are willing to publish.
2. **Store link** — the Download section of `index.html` and its table row point
   at nothing until the AppGallery listing exists.

Then update the `Last updated` date in the policy pages whenever their text changes.

## Preview locally

```sh
python3 -m http.server -d docs 8000   # http://localhost:8000
```

## Keeping the icon in sync

`assets/icon.png` is a copy, not a symlink — GitHub Pages does not follow symlinks.
After running `python3 tools/generate_icons.py`, refresh it:

```sh
cp fastlane/metadata/huawei/images/icon_512.png docs/assets/icon.png
cp fastlane/metadata/huawei/images/icon_216.png docs/assets/icon-216.png
```
