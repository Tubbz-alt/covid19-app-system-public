namespace :build do
  desc "Builds the docker image for the development environment"
  task :devenv do
    include Zuehlke::Execution
    include NHSx::Docker
    docker_out = File.join($configuration.out, "docker")

    mkdir_p(docker_out)
    docker_image_sourcefiles($configuration).each do |f|
      cp_r(f, docker_out)
    end

    begin
      content_tag = full_tag(content_version($configuration))
      pull_repository_image($configuration, content_tag)
      tag_content_version_as_latest($configuration, content_tag)
    rescue GaudiError
      # image doesn't exist, build it
      tags = [DEFAULT_VERSION, content_version($configuration)].map { |x| full_tag(x) }
      tag_cmds = tags.map { |label| "-t #{label}" }.join(" ")
      cmdline = "docker build \"#{docker_out}\" #{tag_cmds}"
      run_command("Build #{tags} container image", cmdline, $configuration)
    end
  end

  desc "Adds the external package dependencies to the lambdas"
  task :dependencies => [:"gen:version", :"build:java"] #, :"build:python"]

  task :batch_creation => [:"gen:proto:java"] do
    java_project_path = File.join($configuration.base, "src/aws/lambdas/incremental_distribution")
    build_gradle_path = File.join(java_project_path, "build.gradle")
    java_output_path = File.join($configuration.out, "build/distributions")
    jar_file = File.join(java_output_path, "javalambda-0.0.1-SNAPSHOT.zip")
    java_src_pattern = "#{$configuration.base}/src/aws/lambdas/incremental_distribution/src/**/*"
    gradlew = File.join(java_project_path, "gradlew")

    file jar_file => Rake::FileList[java_src_pattern, build_gradle_path] do
      cmdline = "#{gradlew} --console plain -p #{java_project_path} clean lambdaZip"
      run_tee("Build incremental distribution lambda", cmdline, $configuration)
    end
    Rake::Task[jar_file].invoke
  end

  task :java => [:"build:batch_creation"]
end
