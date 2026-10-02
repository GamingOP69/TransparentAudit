# Using TransparentAudit entirely from your phone

You can maintain and release this project without having a PC available.

## Edit code

Use GitHub in your mobile browser or the GitHub mobile app to edit files. Commit the change to any branch.

The CI workflow automatically starts for the commit because `ci.yml` watches every branch push.

## Review CI

Open:

`Repository -> Actions -> CI`

You should see a build for the commit/branch you just changed.

## Create a release

Open:

`Repository -> Actions -> Release -> Run workflow`

Enter something like:

```text
v1.2.1
```

Run it. The workflow creates the tag and GitHub Release automatically.

## Important

The phone itself does not run the Java/Maven build. GitHub's hosted runner does. Your phone is only being used to edit the repository and start/review the workflow.
