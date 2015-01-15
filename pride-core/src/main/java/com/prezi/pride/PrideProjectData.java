package com.prezi.pride;

public class PrideProjectData implements Comparable<PrideProjectData> {
	private final String group;
	private final String name;
	private final String path;

	public PrideProjectData(String group, String name, String path) {
		this.group = group;
		this.name = name;
		this.path = path;
	}

	public String getGroup() {
		return group;
	}

	public String getName() {
		return name;
	}

	public String getPath() {
		return path;
	}

	@Override
	@SuppressWarnings("RedundantIfStatement")
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		PrideProjectData that = (PrideProjectData) o;

		if (group != null ? !group.equals(that.group) : that.group != null) return false;
		if (!name.equals(that.name)) return false;
		if (!path.equals(that.path)) return false;

		return true;
	}

	@Override
	public int hashCode() {
		int result = group != null ? group.hashCode() : 0;
		result = 31 * result + name.hashCode();
		result = 31 * result + path.hashCode();
		return result;
	}

	@Override
	public int compareTo(PrideProjectData o) {
		int result = group.compareTo(o.group);
		if (result == 0) {
			result = name.compareTo(o.name);
		}
		if (result == 0) {
			result = path.compareTo(o.path);
		}
		return result;
	}

	@Override
	public String toString() {
		return "PrideProjectData{" +
				"group='" + group + '\'' +
				", name='" + name + '\'' +
				", path='" + path + '\'' +
				'}';
	}
}
