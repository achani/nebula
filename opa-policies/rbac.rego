package nebula.authz

import rego.v1

default allow := false
default reason := "Unknown or denied by default"

# Input is expected to be:
# {
#   "user": {"id": "user1", "roles": ["nebula-admin", "nebula-user"]},
#   "action": "create_project",
#   "resource": "project"
# }

# -----------------
# Admin Rules
# -----------------
allow if {
    "nebula-admin" in input.user.roles
}

reason := "User is a platform administrator" if {
    "nebula-admin" in input.user.roles
}

# -----------------
# Standard User Rules
# -----------------
allow if {
    "nebula-user" in input.user.roles
    input.action == "create_project"
}

reason := "Users are allowed to create projects" if {
    "nebula-user" in input.user.roles
    input.action == "create_project"
}

allow if {
    "nebula-user" in input.user.roles
    input.action == "create_folder"
}

reason := "Users are allowed to create folders" if {
    "nebula-user" in input.user.roles
    input.action == "create_folder"
}

