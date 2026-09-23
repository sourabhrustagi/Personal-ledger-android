package com.ledger.personal.di;

import android.content.Context;
import com.ledger.personal.data.remote.interceptor.AuthInterceptor;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
import dagger.internal.Provider;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;

@ScopeMetadata("javax.inject.Singleton")
@QualifierMetadata("dagger.hilt.android.qualifiers.ApplicationContext")
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
public final class NetworkModule_ProvideOkHttpClientFactory implements Factory<OkHttpClient> {
  private final Provider<AuthInterceptor> authInterceptorProvider;

  private final Provider<HttpLoggingInterceptor> loggingInterceptorProvider;

  private final Provider<Context> contextProvider;

  private NetworkModule_ProvideOkHttpClientFactory(
      Provider<AuthInterceptor> authInterceptorProvider,
      Provider<HttpLoggingInterceptor> loggingInterceptorProvider,
      Provider<Context> contextProvider) {
    this.authInterceptorProvider = authInterceptorProvider;
    this.loggingInterceptorProvider = loggingInterceptorProvider;
    this.contextProvider = contextProvider;
  }

  @Override
  public OkHttpClient get() {
    return provideOkHttpClient(authInterceptorProvider.get(), loggingInterceptorProvider.get(), contextProvider.get());
  }

  public static NetworkModule_ProvideOkHttpClientFactory create(
      Provider<AuthInterceptor> authInterceptorProvider,
      Provider<HttpLoggingInterceptor> loggingInterceptorProvider,
      Provider<Context> contextProvider) {
    return new NetworkModule_ProvideOkHttpClientFactory(authInterceptorProvider, loggingInterceptorProvider, contextProvider);
  }

  public static OkHttpClient provideOkHttpClient(AuthInterceptor authInterceptor,
      HttpLoggingInterceptor loggingInterceptor, Context context) {
    return Preconditions.checkNotNullFromProvides(NetworkModule.INSTANCE.provideOkHttpClient(authInterceptor, loggingInterceptor, context));
  }
}
