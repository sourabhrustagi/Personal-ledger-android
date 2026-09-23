package com.ledger.personal.di;

import com.ledger.personal.data.remote.api.TransactionApiService;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
import dagger.internal.Provider;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import retrofit2.Retrofit;

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
public final class NetworkModule_ProvideTransactionApiServiceFactory implements Factory<TransactionApiService> {
  private final Provider<Retrofit> retrofitProvider;

  private NetworkModule_ProvideTransactionApiServiceFactory(Provider<Retrofit> retrofitProvider) {
    this.retrofitProvider = retrofitProvider;
  }

  @Override
  public TransactionApiService get() {
    return provideTransactionApiService(retrofitProvider.get());
  }

  public static NetworkModule_ProvideTransactionApiServiceFactory create(
      Provider<Retrofit> retrofitProvider) {
    return new NetworkModule_ProvideTransactionApiServiceFactory(retrofitProvider);
  }

  public static TransactionApiService provideTransactionApiService(Retrofit retrofit) {
    return Preconditions.checkNotNullFromProvides(NetworkModule.INSTANCE.provideTransactionApiService(retrofit));
  }
}
