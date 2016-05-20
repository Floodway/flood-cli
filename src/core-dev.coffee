program = require("commander")
path =  require("path")
fs =  require("fs")
Server = require("flood-events").Server



program
  .option("-m","--main","main script location",false)
  .option("-c","--config","use config",false)
  .parse(process.argv)


eventServer = new Server()

if program.m? and program.m != false

  sourcePath = path.join(process.cwd(),program.m)

else
  try
    packageJson  = require(path.join(process.cwd(),"./package.json"))
  catch e
    throw new Error("No package json found!")

  if packageJson?

    index = packageJson["main"]
    sourcePath = path.join(process.cwd(),index)

  else

    throw new Error("No package json found!")

# Load the main file

if program.c? and program.c != false

  config = require(path.join(process.cwd(),program.c))


floodProgram = require(sourcePath)

if config? then floodProgram.config = config

floodProgram.start()

