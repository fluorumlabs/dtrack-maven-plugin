/*
 * Copyright 2021 Artem Godin.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.github.fluorumlabs.dtrackmavenplugin.engine;

import com.google.gson.JsonObject;
import org.apache.maven.plugin.AbstractMojo;
import org.cyclonedx.model.Bom;
import org.cyclonedx.parsers.JsonParser;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Comparator;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.stream.Stream;

public class NpmReactor {
	private static final boolean IS_WINDOWS = System.getProperty("os.name")
			.toLowerCase().startsWith("windows");

	public NpmReactor(AbstractMojo mojo, BomReactor bomReactor) {
		this.mojo = mojo;
		this.bomReactor = bomReactor;
	}

	public void addDependency(String packageName, String versionSpecifier) {
		dependencies.addProperty(packageName, versionSpecifier);
	}

	public void resolveDependencies() {
		if (dependencies.size() > 0) {
			mojo.getLog().info("Collecting additional " + dependencies.size() + " NPM dependencies...");
			JsonObject packageJson = new JsonObject();
			packageJson.addProperty("name", "dtrack-maven-plugin-temp");
			packageJson.addProperty("version", "1.0.0");
			packageJson.addProperty("license", "ISC");
			packageJson.addProperty("description", "");
			packageJson.add("dependencies", dependencies);

			try {
				Path npm = Files.createTempDirectory("npm");
				Path packageJsonFile = npm.resolve("package.json");
				Files.write(packageJsonFile, Collections.singletonList(packageJson.toString()));

				int installResult = exec(npm, "npm install --no-audit --ignore-scripts");
				if (installResult != 0) {
					mojo.getLog().error("NPM INSTALL exited with code " + installResult);
					return;
				}
				int bomResult = exec(npm, "npx @cyclonedx/bom -o bom.json -ns");
				if (bomResult != 0) {
					mojo.getLog().error("@CYCLONE/BOM exited with code " + bomResult);
					return;
				}
				Bom parsedProject = new JsonParser().parse(npm.resolve("bom.json").toFile());
				bomReactor.mergeBom(parsedProject);

				try (Stream<Path> files = Files.walk(npm)) {
					files.sorted(Comparator.reverseOrder())
							.map(Path::toFile)
							.forEach(File::delete);
				}

				dependencies = new JsonObject();
			} catch (Exception e) {
				mojo.getLog().error("Error resolving NPM dependencies", e);
			}
		}
	}

	private int exec(Path npm, String cmd) throws IOException, InterruptedException {
		Process process;
		if (IS_WINDOWS) {
			process = Runtime.getRuntime().exec(new String[]{"cmd.exe", "/c", cmd}, null, npm.toFile());
		} else {
			process = Runtime.getRuntime().exec(new String[]{"sh", "-c", cmd}, null, npm.toFile());
		}
		Executors.newSingleThreadExecutor().submit(new StreamGobbler(process.getInputStream(), mojo.getLog()::info));
		return process.waitFor();
	}

	private JsonObject dependencies = new JsonObject();

	private final AbstractMojo mojo;

	private final BomReactor bomReactor;

	private static final class StreamGobbler implements Runnable {
		private final InputStream inputStream;
		private final Consumer<String> consumer;

		private StreamGobbler(InputStream inputStream, Consumer<String> consumer) {
			this.inputStream = inputStream;
			this.consumer = consumer;
		}

		@Override
		public void run() {
			new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8)).lines()
					.forEach(consumer);
		}
	}
}
