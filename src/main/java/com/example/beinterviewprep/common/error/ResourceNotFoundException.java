package com.example.beinterviewprep.common.error;

public class ResourceNotFoundException extends RuntimeException {

  public ResourceNotFoundException(String resource, Object id) {
    super("%s with id %s was not found".formatted(resource, id));
  }
}
