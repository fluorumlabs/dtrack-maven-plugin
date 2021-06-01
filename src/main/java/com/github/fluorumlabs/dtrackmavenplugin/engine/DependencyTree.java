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

import org.apache.maven.artifact.Artifact;

import java.util.*;
import java.util.function.BiPredicate;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class DependencyTree {
	private final Map<String, Artifact> artifactMap = new HashMap<>();
	private final Map<String, Set<String>> artifactSetMap = new HashMap<>();
	private final Predicate<Artifact> filter;

	public DependencyTree(Collection<Artifact> dependencies, Predicate<Artifact> filter) {
		this.filter = filter;
		for (Artifact artifact : dependencies) {
			artifactMap.put(artifact.getId(), artifact);
			List<String> dependencyTrail = artifact.getDependencyTrail();
			for (int i = dependencyTrail.size() - 1; i > 0; i--) {
				String parent = dependencyTrail.get(i - 1);
				String child = dependencyTrail.get(i);
				artifactSetMap.computeIfAbsent(parent, p -> new HashSet<>()).add(child);
			}
		}
	}

	public List<Artifact> getDirectDependencies(Artifact artifact) {
		return artifactSetMap.getOrDefault(artifact.getId(), Collections.emptySet()).stream()
				.map(artifactMap::get)
				.filter(Objects::nonNull)
				.filter(filter)
				.collect(Collectors.toList());
	}

	public void forEachDependencyPair(Artifact artifact, BiPredicate<Artifact, Artifact> dependencyHandler) {
		Queue<Artifact> queue = new ArrayDeque<>();
		queue.add(artifact);

		while(!queue.isEmpty()) {
			Artifact polledArtifact = queue.poll();
			List<Artifact> directDependencies = getDirectDependencies(polledArtifact);
			queue.addAll(directDependencies.stream().filter(child -> dependencyHandler.test(polledArtifact, child)).collect(Collectors.toList()));
		}
	}
}
