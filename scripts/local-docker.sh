#!/bin/sh

set -u

COMMAND=${1:-start}
APP_HOME=$(CDPATH= cd -P "$(dirname "$0")/.." && pwd) || exit 1
LOCAL_APP_IMAGE=flowit-main-server:local
LOCAL_APP_HEALTH_URL=http://127.0.0.1:8081/actuator/health
LOCAL_INFRASTRUCTURE_SERVICES="mysql redis prometheus grafana"
SOURCE_HASH_LABEL=dev.runtime-lab.flowit.source-hash
ALLOWED_GIT_REMOTE_NAME=origin
ALLOWED_GIT_BRANCH=main
ALLOWED_GIT_REMOTE_URLS="https://github.com/runtime-lab/flowit-main-server.git git@github.com:runtime-lab/flowit-main-server.git"
SOURCE_HASH_PATHS="src/main src/docs src/test/java/dev/runtime_lab/flowit/docs src/test/resources/org/springframework/restdocs build.gradle settings.gradle gradle Dockerfile .dockerignore"

case "$(uname)" in
  Linux*) IS_LINUX=true ;;
  *) IS_LINUX=false ;;
esac

info () {
    printf '%s\n' "$*"
}

fail () {
    printf '\n%s\n\n' "$*" >&2
    exit 1
}

run_quiet_with_timeout () {
    timeout_seconds=$1
    shift

    "$@" >/dev/null 2>&1 &
    command_pid=$!
    elapsed_seconds=0

    while kill -0 "$command_pid" >/dev/null 2>&1; do
        if [ "$elapsed_seconds" -ge "$timeout_seconds" ]; then
            kill "$command_pid" >/dev/null 2>&1 || true
            wait "$command_pid" 2>/dev/null || true
            return 124
        fi

        sleep 1
        elapsed_seconds=$((elapsed_seconds + 1))
    done

    wait "$command_pid"
}

hash_file () {
    if command -v sha256sum >/dev/null 2>&1; then
        sha256sum "$1" | awk '{print $1}'
    elif command -v shasum >/dev/null 2>&1; then
        shasum -a 256 "$1" | awk '{print $1}'
    else
        fail "ERROR: sha256sum or shasum is required to calculate the local source hash."
    fi
}

append_source_hash_entry () {
    source_file=$1
    hash_file_path=$2
    relative_path=${source_file#"$APP_HOME"/}
    file_hash=$(hash_file "$source_file")
    {
        printf '%s\n' "$relative_path"
        printf '%s\n' "$file_hash"
    } >> "$hash_file_path"
}

source_hash () {
    hash_input=${TMPDIR:-/tmp}/flowit-source-hash-input.$$
    : > "$hash_input" || exit 1

    for source_path in $SOURCE_HASH_PATHS; do
        full_path=$APP_HOME/$source_path
        if [ -d "$full_path" ]; then
            find "$full_path" -type f -print | LC_ALL=C sort | while IFS= read -r source_file; do
                append_source_hash_entry "$source_file" "$hash_input"
            done
        elif [ -f "$full_path" ]; then
            append_source_hash_entry "$full_path" "$hash_input"
        fi
    done

    result=$(hash_file "$hash_input")
    rm -f "$hash_input"
    printf '%s' "$result"
}

image_source_hash () {
    value=$(docker image inspect --format "{{ index .Config.Labels \"$SOURCE_HASH_LABEL\" }}" "$LOCAL_APP_IMAGE" 2>/dev/null) || return 1
    case "$value" in
      "" | "<no value>") return 1 ;;
      *) printf '%s' "$value" ;;
    esac
}

usage () {
    info "Usage:"
    info "  ./local.sh start"
    info "  ./local.sh build-image"
    info "  ./local.sh status"
    info "  ./local.sh logs"
    info "  ./local.sh stop"
    info "  ./local.sh infra-start"
    info "  ./local.sh infra-stop"
    info "  ./local.sh docs-refresh"
}

normalize_command () {
    case "${1:-start}" in
      "" | start | build-image | infra-start | infra-stop | stop | status | logs | docs-refresh | help | -h | --help) printf '%s' "${1:-start}" ;;
      localStart) printf '%s' "start" ;;
      localBuildImage | build) printf '%s' "build-image" ;;
      localInfraStart) printf '%s' "infra-start" ;;
      localInfraStop) printf '%s' "infra-stop" ;;
      localStop) printf '%s' "stop" ;;
      localStatus) printf '%s' "status" ;;
      localDocsRefresh | docsRefresh) printf '%s' "docs-refresh" ;;
      *) printf '%s' "$1" ;;
    esac
}

is_truthy () {
    case "$(printf '%s' "${1:-}" | tr '[:upper:]' '[:lower:]')" in
      true | 1 | yes | y | on) return 0 ;;
      *) return 1 ;;
    esac
}

is_production_value () {
    case "$(printf '%s' "${1:-}" | tr '[:upper:]' '[:lower:]')" in
      prod | production) return 0 ;;
      *) return 1 ;;
    esac
}

has_production_profile () {
    for profile in $(printf '%s' "${1:-}" | tr ',;' '  '); do
        if is_production_value "$profile"; then
            return 0
        fi
    done
    return 1
}

add_block_signal () {
    if [ -z "$block_signals" ]; then
        block_signals=$1
    else
        block_signals="$block_signals, $1"
    fi
}

assert_local_docker_allowed () {
    block_signals=
    if is_truthy "${CI:-}"; then
        add_block_signal "CI"
    fi
    if is_truthy "${GITHUB_ACTIONS:-}"; then
        add_block_signal "GITHUB_ACTIONS"
    fi
    if is_truthy "${GITLAB_CI:-}"; then
        add_block_signal "GITLAB_CI"
    fi
    if [ -n "${JENKINS_URL:-}" ]; then
        add_block_signal "JENKINS_URL"
    fi
    if [ -n "${KUBERNETES_SERVICE_HOST:-}" ]; then
        add_block_signal "KUBERNETES_SERVICE_HOST"
    fi
    if is_production_value "${FLOWIT_ENV:-}"; then
        add_block_signal "FLOWIT_ENV=${FLOWIT_ENV}"
    fi
    if has_production_profile "${SPRING_PROFILES_ACTIVE:-}"; then
        add_block_signal "SPRING_PROFILES_ACTIVE=${SPRING_PROFILES_ACTIVE}"
    fi
    if has_production_profile "${SPRING_PROFILES_INCLUDE:-}"; then
        add_block_signal "SPRING_PROFILES_INCLUDE=${SPRING_PROFILES_INCLUDE}"
    fi

    if [ -n "$block_signals" ]; then
        fail "ERROR: refusing to run '$INVOCATION_LABEL'. Local Docker commands are for client/API local development only and are blocked in production or CI environments. Signal(s): $block_signals"
    fi
    if "$IS_LINUX" && ! is_truthy "${FLOWIT_ALLOW_LOCAL_DOCKER:-}"; then
        fail "ERROR: refusing to run '$INVOCATION_LABEL' on Linux without FLOWIT_ALLOW_LOCAL_DOCKER=true. This keeps local Docker commands out of server environments."
    fi
}

require_docker () {
    if ! command -v docker >/dev/null 2>&1; then
        fail "ERROR: Docker is required for '$INVOCATION_LABEL'. Install and start Docker Desktop, then make the 'docker' command available."
    fi

    if ! run_quiet_with_timeout 5 docker info; then
        fail "ERROR: Docker Desktop is not running or the Docker Engine is not ready. Start Docker Desktop or start the Docker Engine, wait until it is running, then run '$INVOCATION_LABEL' again."
    fi

    if ! docker compose version >/dev/null 2>&1; then
        fail "ERROR: Docker Compose is unavailable. Install Docker Desktop or a Docker CLI with the Compose plugin."
    fi
}

confirm_continue_with_current_source () {
    reason=$1

    info "$reason"
    if [ ! -r /dev/tty ] || [ ! -w /dev/tty ]; then
        fail "ERROR: automatic source update did not complete and no interactive terminal is available to confirm continuing with the current source."
    fi

    printf '%s' "Continue with the current local source? [y/N] " > /dev/tty
    IFS= read -r answer < /dev/tty || answer=
    case "$(printf '%s' "$answer" | tr '[:upper:]' '[:lower:]')" in
      y | yes)
        info "Continuing with current local source."
        return 0
        ;;
      *)
        fail "ERROR: stopped because local source was not updated."
        ;;
    esac
}

sync_local_source_if_needed () {
    case "$COMMAND" in
      start | build-image) ;;
      *) return 0 ;;
    esac

    if is_truthy "${FLOWIT_SKIP_AUTO_UPDATE:-}"; then
        info "Local source update skipped because FLOWIT_SKIP_AUTO_UPDATE is set."
        return 0
    fi

    if ! command -v git >/dev/null 2>&1; then
        info "Git is unavailable; skipping local source update."
        return 0
    fi

    git_top_level=$(git -C "$APP_HOME" rev-parse --show-toplevel 2>/dev/null) || {
        confirm_continue_with_current_source "Could not read Git repository metadata. Automatic source update cannot verify whether this source is current."
        return 0
    }
    if [ "$(CDPATH= cd -P "$git_top_level" && pwd)" != "$APP_HOME" ]; then
        info "Git repository root is not the expected project root; skipping automatic update. Expected $APP_HOME, got $git_top_level."
        return 0
    fi

    remote_url=$(git -C "$APP_HOME" remote get-url "$ALLOWED_GIT_REMOTE_NAME" 2>/dev/null) || {
        info "Configured remote '$ALLOWED_GIT_REMOTE_NAME' is unavailable; skipping automatic update."
        return 0
    }
    allowed_remote=false
    for allowed_url in $ALLOWED_GIT_REMOTE_URLS; do
        if [ "$remote_url" = "$allowed_url" ]; then
            allowed_remote=true
            break
        fi
    done
    if [ "$allowed_remote" != true ]; then
        return 0
    fi

    current_branch=$(git -C "$APP_HOME" branch --show-current 2>/dev/null) || {
        fail "ERROR: repository uses allowed remote '$ALLOWED_GIT_REMOTE_NAME', but is not on a named branch. Switch to '$ALLOWED_GIT_BRANCH' or set FLOWIT_SKIP_AUTO_UPDATE=true when intentionally running without source update."
    }
    if [ -z "$current_branch" ]; then
        fail "ERROR: repository uses allowed remote '$ALLOWED_GIT_REMOTE_NAME', but is not on a named branch. Switch to '$ALLOWED_GIT_BRANCH' or set FLOWIT_SKIP_AUTO_UPDATE=true when intentionally running without source update."
    fi
    if [ "$current_branch" != "$ALLOWED_GIT_BRANCH" ]; then
        fail "ERROR: current branch is '$current_branch', but '$ALLOWED_GIT_REMOTE_NAME' points to the allowed source. Switch to '$ALLOWED_GIT_BRANCH' before running $INVOCATION_LABEL."
    fi

    remote_branch="$ALLOWED_GIT_REMOTE_NAME/$ALLOWED_GIT_BRANCH"

    info "Checking for source updates from $remote_branch..."
    if ! git -C "$APP_HOME" fetch --prune --quiet "$ALLOWED_GIT_REMOTE_NAME" "$ALLOWED_GIT_BRANCH"; then
        confirm_continue_with_current_source "Could not fetch source updates from $remote_branch."
        return 0
    fi

    counts=$(git -C "$APP_HOME" rev-list --left-right --count "HEAD...$remote_branch" 2>/dev/null) || {
        confirm_continue_with_current_source "Could not compare local source with $remote_branch."
        return 0
    }

    set -- $counts
    ahead=${1:-0}
    behind=${2:-0}

    if [ "$behind" -eq 0 ]; then
        info "Local source is up to date with $remote_branch."
        return 0
    fi

    status_output=$(git -C "$APP_HOME" status --porcelain 2>/dev/null) || {
        confirm_continue_with_current_source "Could not inspect local source changes before updating from $remote_branch."
        return 0
    }
    if [ -n "$status_output" ]; then
        confirm_continue_with_current_source "Upstream source has $behind newer commit(s), but local changes are present. Commit or stash local changes, then run: git fetch $ALLOWED_GIT_REMOTE_NAME $ALLOWED_GIT_BRANCH; git merge --ff-only $remote_branch"
        return 0
    fi

    if [ "$ahead" -gt 0 ]; then
        confirm_continue_with_current_source "Local branch and $remote_branch have diverged. Resolve the branch manually, then run: git fetch $ALLOWED_GIT_REMOTE_NAME $ALLOWED_GIT_BRANCH; git merge --ff-only $remote_branch"
        return 0
    fi

    info "Updating local source from $remote_branch ($behind commit(s))..."
    if git -C "$APP_HOME" merge --ff-only "$remote_branch"; then
        info "Local source updated."
    else
        confirm_continue_with_current_source "Automatic source update failed. Run manually when ready: git fetch $ALLOWED_GIT_REMOTE_NAME $ALLOWED_GIT_BRANCH; git merge --ff-only $remote_branch"
    fi
}

docker_compose () {
    if "$IS_LINUX"; then
        docker compose -f "$APP_HOME/compose.yaml" -f "$APP_HOME/compose.linux.yaml" "$@"
    else
        docker compose -f "$APP_HOME/compose.yaml" "$@"
    fi
}

docker_build () {
    build_source_hash=${1:-$(source_hash)}
    docker build --progress=plain --label "$SOURCE_HASH_LABEL=$build_source_hash" -t "$LOCAL_APP_IMAGE" -f "$APP_HOME/Dockerfile" "$APP_HOME"
}

curl_health () {
    if command -v curl >/dev/null 2>&1; then
        curl -fsS "$LOCAL_APP_HEALTH_URL" >/dev/null 2>&1
    elif command -v wget >/dev/null 2>&1; then
        wget -q -O /dev/null "$LOCAL_APP_HEALTH_URL" >/dev/null 2>&1
    else
        return 2
    fi
}

wait_for_health () {
    if ! command -v curl >/dev/null 2>&1 && ! command -v wget >/dev/null 2>&1; then
        info "Health check skipped because neither curl nor wget is available."
        return 0
    fi

    info "Waiting up to 90 seconds for application health..."
    deadline=$(( $(date +%s) + 90 ))
    while [ "$(date +%s)" -lt "$deadline" ]; do
        if curl_health; then
            info "Health: $LOCAL_APP_HEALTH_URL (HTTP 200)"
            return 0
        fi
        sleep 1
    done

    info "Health endpoint is not ready yet: $LOCAL_APP_HEALTH_URL"
}

local_start () {
    info "Checking local source hash..."
    current_source_hash=$(source_hash)
    if docker image inspect "$LOCAL_APP_IMAGE" >/dev/null 2>&1; then
        image_exists=true
        current_image_source_hash=$(image_source_hash || true)
    else
        image_exists=false
        current_image_source_hash=
    fi

    if [ "$current_image_source_hash" = "$current_source_hash" ]; then
        info "Source hash unchanged; reusing image."
        info "Using existing Docker image: $LOCAL_APP_IMAGE"
        info "Run $BUILD_IMAGE_LABEL when you need to rebuild the application image from source."
    elif "$image_exists"; then
        if [ -z "$current_image_source_hash" ]; then
            info "Docker image source hash is missing; rebuilding Docker image: $LOCAL_APP_IMAGE"
        else
            info "Source hash changed; rebuilding image."
            info "Rebuilding Docker image: $LOCAL_APP_IMAGE"
        fi
        docker_build "$current_source_hash" || exit $?
    else
        info "Docker image not found: $LOCAL_APP_IMAGE"
        info "Building Docker image: $LOCAL_APP_IMAGE"
        docker_build "$current_source_hash" || exit $?
    fi

    docker_compose up -d || exit $?

    info "Flowit local Docker Compose stack start requested."
    info "Application: http://127.0.0.1:8080"
    wait_for_health
    info "Logs: docker compose logs -f app"
}

refresh_docs () {
    info "Refreshing API docs without restarting the local application..."
    "$APP_HOME/gradlew" copyApiDocs --rerun-tasks -PforceApiDocs=true || exit $?
    info "API docs refreshed: build/resources/main/static/docs"
    info "Reload http://127.0.0.1:8080/docs/index.html in the browser."
    info "When the app runs in Docker, refreshed docs are visible after reload if it was started with the default docs mount."
}

COMMAND=$(normalize_command "$COMMAND")
if [ -n "${FLOWIT_GRADLE_FALLBACK_TASK:-}" ]; then
    INVOCATION_LABEL="./gradlew $FLOWIT_GRADLE_FALLBACK_TASK"
    BUILD_IMAGE_LABEL="./gradlew localBuildImage"
else
    INVOCATION_LABEL="./local.sh $COMMAND"
    BUILD_IMAGE_LABEL="./local.sh build-image"
fi

case "$COMMAND" in
  help | -h | --help)
    usage
    exit 0
    ;;
esac

case "$COMMAND" in
  start | build-image | infra-start | infra-stop | stop | status | logs | docs-refresh) ;;
  *)
    usage
    fail "ERROR: unsupported local Docker command: $COMMAND"
    ;;
esac

if [ "$COMMAND" = "docs-refresh" ]; then
    refresh_docs
    exit 0
fi

assert_local_docker_allowed
require_docker
sync_local_source_if_needed
if [ -n "${FLOWIT_GRADLE_FALLBACK_TASK:-}" ]; then
    info "Local Java is unavailable; running './gradlew $FLOWIT_GRADLE_FALLBACK_TASK' through Docker commands."
fi

case "$COMMAND" in
  start)
    local_start
    ;;
  build-image)
    current_source_hash=$(source_hash)
    info "Building Docker image: $LOCAL_APP_IMAGE"
    docker_build "$current_source_hash"
    ;;
  infra-start)
    docker_compose up -d --wait $LOCAL_INFRASTRUCTURE_SERVICES || {
        info "Docker Compose did not accept or complete --wait. Falling back to: docker compose up -d"
        docker_compose up -d $LOCAL_INFRASTRUCTURE_SERVICES
    }
    ;;
  infra-stop)
    docker_compose stop $LOCAL_INFRASTRUCTURE_SERVICES
    ;;
  stop)
    docker_compose down
    ;;
  status)
    if curl_health; then
        info "Application health: HTTP 200"
    else
        info "Application health: unavailable"
    fi
    info ""
    info "Docker Compose services:"
    docker_compose ps
    ;;
  logs)
    docker_compose logs -f app
    ;;
esac
