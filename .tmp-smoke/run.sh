#!/usr/bin/env sh
# Forge requires a configured set of both JVM and program arguments.
# Add custom JVM arguments to the user_jvm_args.txt
# Add custom program arguments {such as nogui} to this file in the next line before the "$@" or
#  pass them to this script directly
exec java @user_jvm_args.txt @libraries/net/neoforged/neoforge/26.1.2.73/unix_args.txt "$@"
