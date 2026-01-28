# CogniHertz Documentation

This directory contains documentation for CogniHertz, including legal documents required for Google Play Store submission.

## GitHub Pages

This directory is configured for GitHub Pages hosting. The legal documents are accessible at:

- **Privacy Policy:** https://matthewfrazier.github.io/gammasync/privacy-policy
- **Terms of Service:** https://matthewfrazier.github.io/gammasync/terms-of-service
- **Documentation Home:** https://matthewfrazier.github.io/gammasync/

## Enabling GitHub Pages

GitHub Pages must be enabled in the repository settings:

1. Go to repository **Settings**
2. Navigate to **Pages** (left sidebar)
3. Under **Source**, select:
   - Source: **Deploy from a branch**
   - Branch: **main**
   - Folder: **/docs**
4. Click **Save**

GitHub will build and deploy the site within a few minutes. The URLs above will become accessible once deployment completes.

## Documents

### Legal Documents (Required for Play Store)
- `privacy-policy.md` - Privacy policy stating no data collection
- `terms-of-service.md` - Terms of service with disclaimers and safety warnings

### Technical Documentation
- `CI_ARTIFACTS.md` - CI artifact best practices
- `FIREBASE_SETUP.md` - Firebase App Distribution setup
- `RECENT_FIXES.md` - Recent bug fixes
- `ENGINEERING_BLUEPRINT.md` - Engineering specifications
- `CLAUDE_CODE_GITHUB_ACTIONS.md` - GitHub Actions integration
- `CLAUDE_CODE_SUBAGENTS.md` - Custom subagent creation
- `mcp-proposal.md` - MCP integration proposal

### Site Configuration
- `_config.yml` - Jekyll configuration for GitHub Pages
- `index.md` - Documentation homepage
- `README.md` - This file

## Local Preview

To preview the site locally:

```bash
# Install Jekyll (requires Ruby)
gem install jekyll bundler

# Serve the site
cd docs
jekyll serve --baseurl /gammasync

# Open http://localhost:4000/gammasync/
```

## Updating Legal Documents

When updating legal documents, remember to:
1. Update the "Last Updated" date
2. Commit and push changes
3. GitHub Pages will automatically rebuild (takes 1-5 minutes)
4. Verify changes at the public URLs
