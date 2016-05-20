#!/usr/bin/env node
var chalk, program;

chalk = require("chalk");

program = require("commander");

program.version(require("../package.json")["version"]).command("init <name> ", "create a new Floodway Project").command("generate-java", "generate Java Android Classes").command("generate-docs", "generate API Documentation").command("dev", "run a dev. instance of the current application").parse(process.argv);
