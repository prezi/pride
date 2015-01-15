package com.prezi.pride;

public interface Named {
	String getName();

	interface Namer<T> {
		String getName(T value);
	}

	static Namer<Named> NAMED_NAMER = new Namer<Named>() {
		@Override
		public String getName(Named value) {
			return value.getName();
		}
	};

	static Namer<Object> TOSTRING_NAMER = new Namer<Object>() {
		@Override
		public String getName(Object value) {
			return String.valueOf(value);
		}
	};
}
