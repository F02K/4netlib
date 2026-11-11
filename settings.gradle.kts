rootProject.name = "4netlib"

// Core plugin
include("safety-spy")

// Modules - each builds its own jar, dropped into plugins/4netlib/modules/
include("modules:safety")
include("modules:chat")
include("modules:actionbar")
