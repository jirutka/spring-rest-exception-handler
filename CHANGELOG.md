# Changelog

## 1.0.3 (2015-03-31)

*  Fix error when deserializing `ErrorMessage` from JSON using Jackson 2 due to multiple setters for the status property (thanks to @lukasniemeier-zalando). [[#6](https://github.com/jirutka/spring-rest-exception-handler/pull/6)]

## 1.0.2 (2015-02-07)

*  Modify `ErrorMessageRestExceptionHandler` to log missing message on the level INFO instead of WARN. [[#3](https://github.com/jirutka/spring-rest-exception-handler/issues/3)]
*  Fix compile error when Jackson 2 is not on the classpath.
*  Fix problem with missing `MappingJacksonHttpMessageConverter` on Spring 4.1.0 and greater.

## 1.0.1 (2014-06-19)

*  Add exception handler for `ConstraintViolationException` from the Bean Validation (JSR 303/349).
*  Fix message key of detail for `MethodArgumentNotValidException`.
*  Fix content negotiation to prefer the specified default content type when client doesn’t provide the Accept header. [[#2](https://github.com/jirutka/spring-rest-exception-handler/issues/2)]
*  Improve integration tests.

## 1.0 (2014-04-29)

First stable release.
