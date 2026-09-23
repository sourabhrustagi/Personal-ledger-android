package com.ledger.personal.data.repository;

import com.ledger.personal.data.remote.api.TransactionApiService;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Provider;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import kotlinx.coroutines.CoroutineDispatcher;

@ScopeMetadata("javax.inject.Singleton")
@QualifierMetadata
@DaggerGenerated
@Generated(
    value = "dagger.internal.codegen.ComponentProcessor",
    comments = "https://dagger.dev"
)
@SuppressWarnings({
    "unchecked",
    "rawtypes",
    "KotlinInternal",
    "KotlinInternalInJava",
    "cast",
    "deprecation",
    "nullness:initialization.field.uninitialized"
})
public final class RetrofitTransactionRepository_Factory implements Factory<RetrofitTransactionRepository> {
  private final Provider<TransactionApiService> apiServiceProvider;

  private final Provider<CoroutineDispatcher> ioDispatcherProvider;

  private RetrofitTransactionRepository_Factory(Provider<TransactionApiService> apiServiceProvider,
      Provider<CoroutineDispatcher> ioDispatcherProvider) {
    this.apiServiceProvider = apiServiceProvider;
    this.ioDispatcherProvider = ioDispatcherProvider;
  }

  @Override
  public RetrofitTransactionRepository get() {
    return newInstance(apiServiceProvider.get(), ioDispatcherProvider.get());
  }

  public static RetrofitTransactionRepository_Factory create(
      Provider<TransactionApiService> apiServiceProvider,
      Provider<CoroutineDispatcher> ioDispatcherProvider) {
    return new RetrofitTransactionRepository_Factory(apiServiceProvider, ioDispatcherProvider);
  }

  public static RetrofitTransactionRepository newInstance(TransactionApiService apiService,
      CoroutineDispatcher ioDispatcher) {
    return new RetrofitTransactionRepository(apiService, ioDispatcher);
  }
}
