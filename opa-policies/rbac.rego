package nebula.authz

import rego.v1

default allow := false

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

# -----------------
# Standard User Rules
# -----------------
allow if {
    "nebula-user" in input.user.roles
    input.action == "create_project"
}

allow if {
    "nebula-user" in input.user.roles
    input.action == "create_folder"
}

# -----------------
# Code Service Rules
# -----------------
allow if {
    "nebula-user" in input.user.roles
    input.action == "code:repository:create"
}

allow if {
    "nebula-user" in input.user.roles
    input.action == "code:repository:read"
}

allow if {
    "nebula-user" in input.user.roles
    input.action == "view"
    startswith(input.resource, "project:")
}

allow if {
    "nebula-user" in input.user.roles
    input.action == "edit"
    startswith(input.resource, "project:")
}

# -----------------
# Reason Logic
# -----------------
reason := "User is a platform administrator" if {
    "nebula-admin" in input.user.roles
} else := "Users are allowed to create projects" if {
    "nebula-user" in input.user.roles
    input.action == "create_project"
} else := "Users are allowed to create folders" if {
    "nebula-user" in input.user.roles
    input.action == "create_folder"
} else := "Users are allowed to create repositories" if {
    "nebula-user" in input.user.roles
    input.action == "code:repository:create"
} else := "Users are allowed to read repositories" if {
    "nebula-user" in input.user.roles
    input.action == "code:repository:read"
} else := "Users are allowed to view projects" if {
    "nebula-user" in input.user.roles
    input.action == "view"
    startswith(input.resource, "project:")
} else := "Unknown or denied by default"
