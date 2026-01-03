FROM scratch

ARG APPLICATION_VERSION

LABEL org.opencontainers.image.title="atto-signer" \
      org.opencontainers.image.description="Atto signer built as a static GraalVM image" \
      org.opencontainers.image.url="https://atto.cash" \
      org.opencontainers.image.source="https://github.com/attocash/signer" \
      org.opencontainers.image.version="${APPLICATION_VERSION}"

ENV APPLICATION_VERSION=${APPLICATION_VERSION}

COPY ./build/native/nativeCompile/signer /app/signer

WORKDIR /tmp

WORKDIR /app

USER 65532:65532

EXPOSE 8080

ENTRYPOINT ["/app/signer"]
