# Holobot - Deployment

This README documents how to set up the VM to automatically deploy Holobot using Docker.

## Automated release & deployment

Deployment is fully automated once the VM is set up:

1. Pushing to `main` lets [Release Please](../.github/workflows/release-please.yml) open or update a release PR.
2. Merging that PR creates a GitHub release, which triggers [`docker.yml`](../.github/workflows/docker.yml).
3. That workflow builds the image with Jib, pushes it to GHCR, then SSHes into the VM to run `docker compose pull` and `docker compose up -d`, using the tag from the release.

The SSH deploy step requires the `VM_HOST`, `VM_USER`, `VM_SSH_KEY`, and `VM_SSH_PORT` repository secrets to be configured in GitHub Actions.

## One-Time VM Setup

1. Install Docker & Docker Compose

```bash
sudo apt update
sudo apt install -y docker.io docker-compose-plugin
sudo systemctl enable docker
sudo systemctl start docker
```

2. Create Holobot directory structure

```bash
mkdir -p /home/ubuntu/holo/{data,logs}
cd /home/ubuntu/holo
```

3. Copy `docker-compose.yml` from this repository to

```bash
/home/ubuntu/holo/docker-compose.yml
```

4. Create `/home/ubuntu/holo/.env` from [`.env.example`](../.env.example) in this repository, filling in the bot token and other required values.

The `IMAGE_TAG` value in `.env` only matters for manual `docker compose` runs — the automated deploy step above sets it from the release tag before pulling.