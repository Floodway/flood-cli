#!/usr/bin/env node
var Server, eventServer, fs, path, program;

program = require("commander");

path = require("path");

fs = require("fs");

Server = require("flood-events").Server;

program.option("-m", "--main", "main script location", false).option("-c", "--config", "use config", false).parse(process.argv);

eventServer = new Server(function() {
  var config, e, error, floodProgram, index, packageJson, sourcePath;
  if ((program.m != null) && program.m !== false) {
    sourcePath = path.join(process.cwd(), program.m);
  } else {
    try {
      packageJson = require(path.join(process.cwd(), "./package.json"));
    } catch (error) {
      e = error;
      throw new Error("No package json found!");
    }
    if (packageJson != null) {
      index = packageJson["main"];
      sourcePath = path.join(process.cwd(), index);
    } else {
      throw new Error("No package json found!");
    }
  }
  if ((program.c != null) && program.c !== false) {
    config = require(path.join(process.cwd(), program.c));
  }
  floodProgram = require(sourcePath);
  if (config != null) {
    floodProgram.config = config;
  }
  return floodProgram.start();
});
