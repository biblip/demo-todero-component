#!/usr/bin/env bash
set -euo pipefail

workspace_dir="${1:-./workspace}"
component_name="${2:-com.shellaia.verbatim.agent.dj}"
command_name="${3:-process}"
body_value="${4:-get status}"
component_dir_name="todero-component-template"
agent_dir_name="agent"

component_jar_path="$(ls -t component/target/todero-component-template-*.jar 2>/dev/null | head -n 1 || true)"
agent_jar_path="$(ls -t agent/target/agent-*.jar 2>/dev/null | head -n 1 || true)"
if [[ -z "${component_jar_path}" || -z "${agent_jar_path}" ]]; then
  mvn -pl component,agent -am clean package
  component_jar_path="$(ls -t component/target/todero-component-template-*.jar 2>/dev/null | head -n 1 || true)"
  agent_jar_path="$(ls -t agent/target/agent-*.jar 2>/dev/null | head -n 1 || true)"
  if [[ -z "${component_jar_path}" ]]; then
    echo "No component JAR found in component/target/. Build failed." >&2
    exit 1
  fi
  if [[ -z "${agent_jar_path}" ]]; then
    echo "No agent JAR found in agent/target/. Build failed." >&2
    exit 1
  fi
fi

mkdir -p "${workspace_dir}/components/${component_dir_name}"
cp "${component_jar_path}" "${workspace_dir}/components/${component_dir_name}/todero-component-template.jar"
mkdir -p "${workspace_dir}/components/${agent_dir_name}"
cp "${agent_jar_path}" "${workspace_dir}/components/${agent_dir_name}/agent.jar"

echo "Copied ${component_jar_path} -> ${workspace_dir}/components/${component_dir_name}/todero-component-template.jar"
echo "Copied ${agent_jar_path} -> ${workspace_dir}/components/${agent_dir_name}/agent.jar"

shift 4 || true

java "-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=*:5005" \
  -jar ./todero-runner.jar \
  --workspace-dir "${workspace_dir}" \
  --server-type AI \
  --component "${component_name}" \
  --command "${command_name}" \
  --body "${body_value}" \
  "$@"
