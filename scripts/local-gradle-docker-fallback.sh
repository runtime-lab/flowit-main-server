#!/bin/sh

set -u

TASK=${1:-}
APP_HOME=$(CDPATH= cd -P "$(dirname "$0")/.." && pwd) || exit 1
LOCAL_APP_IMAGE=flowit-main-server:local
LOCAL_APP_HEALTH_URL=http://127.0.0.1:8081/actuator/health
LOCAL_INFRASTRUCTURE_SERVICES="mysql redis prometheus grafana"

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
        fail "ERROR: refusing to run './gradlew $TASK'. Local Docker tasks are for client/API local development only and are blocked in production or CI environments. Signal(s): $block_signals"
    fi
    if "$IS_LINUX" && ! is_truthy "${FLOWIT_ALLOW_LOCAL_DOCKER:-}"; then
        fail "ERROR: refusing to run './gradlew $TASK' on Linux without FLOWIT_ALLOW_LOCAL_DOCKER=true. This keeps local Docker tasks out of server environments."
    fi
}

require_docker () {
    if ! command -v docker >/dev/null 2>&1; then
        fail "ERROR: local Java is unavailable, and Docker is required for './gradlew $TASK'. Install Java or start Docker Desktop and make the 'docker' command available."
    fi

    if ! docker compose version >/dev/null 2>&1; then
        fail "ERROR: Docker Compose is unavailable. Install Docker Desktop or a Docker CLI with the Compose plugin."
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
    docker build --progress=plain -t "$LOCAL_APP_IMAGE" -f "$APP_HOME/Dockerfile" "$APP_HOME"
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
    if docker image inspect "$LOCAL_APP_IMAGE" >/dev/null 2>&1; then
        info "Using existing Docker image: $LOCAL_APP_IMAGE"
        info "Run ./gradlew localBuildImage when you need to rebuild the application image from source."
    else
        info "Docker image not found: $LOCAL_APP_IMAGE"
        info "Building Docker image: $LOCAL_APP_IMAGE"
        docker_build || exit $?
    fi

    docker_compose up -d || exit $?

    info "Flowit local Docker Compose stack start requested."
    info "Application: http://127.0.0.1:8080"
    wait_for_health
    info "Logs: docker compose logs -f app"
}

assert_local_docker_allowed
require_docker
info "Local Java is unavailable; running './gradlew $TASK' through Docker commands."

case "$TASK" in
  localStart)
    local_start
    ;;
  localBuildImage)
    info "Building Docker image: $LOCAL_APP_IMAGE"
    docker_build
    ;;
  localInfraStart)
    docker_compose up -d --wait $LOCAL_INFRASTRUCTURE_SERVICES || {
        info "Docker Compose did not accept or complete --wait. Falling back to: docker compose up -d"
        docker_compose up -d $LOCAL_INFRASTRUCTURE_SERVICES
    }
    ;;
  localInfraStop)
    docker_compose stop $LOCAL_INFRASTRUCTURE_SERVICES
    ;;
  localStop)
    docker_compose down
    ;;
  localStatus)
    if curl_health; then
        info "Application health: HTTP 200"
    else
        info "Application health: unavailable"
    fi
    info ""
    info "Docker Compose services:"
    docker_compose ps
    ;;
  *)
    fail "ERROR: unsupported Docker fallback task: $TASK"
    ;;
esac
