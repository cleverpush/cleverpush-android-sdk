package com.cleverpush;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;

import kotlin.io.TextStreamsKt;
import kotlin.jvm.internal.Intrinsics;

public class MockResponseFileReader {

    private final String content;

    public final String getContent() {
        return this.content;
    }

    public MockResponseFileReader(String path) {
        Intrinsics.checkParameterIsNotNull(path, "path");

        InputStreamReader reader = new InputStreamReader(this.getClass().getClassLoader().getResourceAsStream(path));
        this.content = TextStreamsKt.readText((Reader)reader);
        try {
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
