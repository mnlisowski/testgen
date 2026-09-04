package com.mlisows.testgen.usecase.ports;

import com.mlisows.testgen.domain.profile.ObservedProfile;

import java.nio.file.Path;

public interface ObservedProfileReader {
    ObservedProfile read(Path profilePath);
}
