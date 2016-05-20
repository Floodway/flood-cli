program = require("commander")
path =  require("path")
fs =  require("fs")

ensure = require("is_js")

program
  .option("-m","--main","main script location")
  .parse(process.argv)

# Generate a java doc




if program.m?

  sourcePath = path.join(process.cwd(),program.m)

else

  # Try loading the package.json

  packageJson  = require(path.join(process.cwd(),"./package.json"))

  if packageJson?

    index = packageJson["main"]


    sourcePath = path.join(process.cwd(),index)

    javaPath = packageJson["javaPath"]

    if javaPath?

      javaPath = path.join(process.cwd(),javaPath)

    console.log "Java path: "+javaPath


    if packageJson.javaPackage?
      javaPackage = "package  "+packageJson.javaPackage+";"
    else
      javaPackage = "//TODO: Specify package"


    javaPackage += "\n"


  else

    throw new Error("No package json found!")


# Load the main file



program = require(sourcePath)

makeClassName = (string) -> return string.charAt(0).toUpperCase()+string.substring(1)

getVarType = (options) ->

  if options.useClass? then return makeClassName(options.useClass)

  if ensure.string(options) then return makeClassName(options+"Schema")

  return switch options.type
    when "number"
      if options.useInt then "int" else "long"
    when "boolean" then return "boolean"
    when "string" then return "String"
    when "array"
      if options.mode == "unique"
        throw new Error("Could not convert list with unique mode. Not supported in JAVA!")
      else
        return "List<#{ getVarType(options.children)}>"

    when "object"

      if ensure.string(options.children)
        return makeClassName(options.children+"Schema")


      throw new Error("Unable to transform object, use a schema")
      #throw new Error("Could not convert type at: ",name,"of",options," please add a useClass annotation")
    else

      if options.useType?
        return options.useType
      else
        console.error(options)
        throw new Error("Unable to transform type, use a useType annotation")
        #throw new Error("Could not convert type at: ",name,"of",options," please add a useType annotation")

convertSchema = (config,indent) ->

  result = ""

  if ensure.string(config) then return makeClassName(config+"Schema")


  for name,options of config

    varType = getVarType(options)

    result += indent+"public "+varType+" "+name+";\n"

  return result



if program.isFloodProcessor? and program.isFloodProcessor() or true

  # Retrieve necessary information

  apiConfigFile = fs.readFileSync(path.join(__dirname,"../data/ApiConfig.java")).toString()

  replacing = "" 

  i = "    "

  for name,ns of program.namespaces

    namespaceClassName = makeClassName(name)

    replacing += i+"public static class #{ namespaceClassName }{\n"

    for schemaName, schema of ns.schemas

      replacing += i+i+"public static class "+makeClassName(schemaName)+"Schema{\n"

      replacing += convertSchema(schema,i+i+i)

      replacing += i+i+"}\n\n"

    for actionName, action of ns.actions

      if not action.skipJava

        actionClassName = makeClassName(actionName)

        if not ensure.string(action.params)


          if Object.keys(action.params).length != 0

            noParams = false

            paramsClass =  actionClassName+"Params"

            replacing += i+i+"public static class "+paramsClass+"{\n"

            # Convert schema
            replacing += convertSchema(action.params,i+i+i)

            replacing += i+i+"}\n\n"

          else

            noParams = true

        else
          noParams = false
          paramsClass = makeClassName(action.params+"Schema")

        if not ensure.string(action.result)

          resultClass = actionClassName+"Result"

          replacing += i+i+"public static class "+resultClass+"{\n"

          replacing += convertSchema(action.result,i+i+i)


          replacing += ""+i+i+"}\n\n"



        else

          resultClass = makeClassName(action.result+"Schema")


        # Add method
        if not noParams
          replacing += i+i+"public static Request<#{resultClass}> #{actionName}(#{paramsClass} params, final ActionCallback<#{ resultClass }> callback){\n"
        else
          replacing += i+i+"public static Request<#{resultClass}> #{actionName}(final ActionCallback<#{ resultClass }> callback){\n"

        replacing += i+i+i+"return new Request<#{ resultClass }>(\"#{ name }\",\"#{ actionName }\", new Request.RequestCallback(){\n"
        replacing += i+i+i+i+"@Override\n"
        replacing += i+i+i+i+"public void onResult(String result){\n"
        replacing += i+i+i+i+i+"callback.onResult(gson.fromJson(result,#{resultClass}.class));\n"
        replacing += i+i+i+i+i+"}\n"
        replacing += i+i+i+i+i+"@Override\n"
        replacing += i+i+i+i+i+"public void onError(String error){callback.onError(error);}\n"
        if noParams
          replacing += i+i+i+"});\n"
        else
          replacing += i+i+i+"},params);\n"
        replacing += i+i+"}\n\n"



    replacing += i+"}\n"


  # Writing...

  if not javaPath?

    if not fs.existsSync(path.join(process.cwd(),"./generated/"))

      fs.mkdirSync(path.join(process.cwd(),"./generated/"))


      javaPath = path.join(process.cwd(),"./generated/")


  fs.writeFileSync(javaPath+"ApiConfig.java",javaPackage+apiConfigFile.replace("//REPLACEME//",replacing))
  fs.writeFileSync(javaPath+"ApiBase.java",javaPackage+fs.readFileSync(path.join(__dirname,"../data/ApiBase.java")).toString())
  fs.writeFileSync(javaPath+"Request.java",javaPackage+fs.readFileSync(path.join(__dirname,"../data/Request.java")).toString())





