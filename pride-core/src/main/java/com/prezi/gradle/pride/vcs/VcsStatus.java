package com.prezi.gradle.pride.vcs;

/**
 * Represents the VCS status of a module.
 */
public final class VcsStatus {
	private final String branch;
	private final String revision;
	private final boolean hasUnpublishedChanges;
	private final boolean hasUncommittedChanges;

	public static Builder builder(String revision) {
		return new Builder(revision);
	}

	private VcsStatus(String revision, String branch, boolean hasUnpublishedChanges, boolean hasUncommittedChanges) {
		this.revision = revision;
		this.branch = branch;
		this.hasUnpublishedChanges = hasUnpublishedChanges;
		this.hasUncommittedChanges = hasUncommittedChanges;
	}

	/**
	 * Returns the name of the current branch or <code>null</code> if not on a branch.
	 */
	public String getBranch() {
		return branch;
	}

	/**
	 * Returns the current revision the working copy is pointing at.
	 */
	public String getRevision() {
		return revision;
	}

	/**
	 * Returns <code>true</code> if there are unpublished changes.
	 */
	public boolean hasUnpublishedChanges() {
		return hasUnpublishedChanges;
	}

	/**
	 * Returns <code>true</code> if there are uncommitted changes.
	 */
	public boolean hasUncommittedChanges() {
		return hasUncommittedChanges;
	}

	public static final class Builder {
		private final String revision;
		private String branch;
		private boolean hasUnpublishedChanges;
		private boolean hasUncommittedChanges;

		public Builder(String revision) {
			this.revision = revision;
		}

		public VcsStatus build() {
			return new VcsStatus(revision, branch, hasUnpublishedChanges, hasUncommittedChanges);
		}

		public Builder withBranch(String branch) {
			this.branch = branch;
			return this;
		}

		public Builder withUnpublishedChanges(boolean hasUnpublishedChanges) {
			this.hasUnpublishedChanges = hasUnpublishedChanges;
			return this;
		}

		public Builder withUncommittedChanges(boolean hasCommittedChanges) {
			this.hasUncommittedChanges = hasCommittedChanges;
			return this;
		}
	}
}
