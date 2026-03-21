package com.crypto_data_platform.crypto_data_platform;

import com.crypto_data_platform.crypto_data_platform.client.CryptoApiClient;
import com.crypto_data_platform.crypto_data_platform.domain.CryptoPrice;
import com.crypto_data_platform.crypto_data_platform.repository.CryptoRepository;
import com.crypto_data_platform.crypto_data_platform.service.CryptoService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class CryptoDataPlatformApplication {

    public static void main(String[] args) {
        SpringApplication.run(CryptoDataPlatformApplication.class, args);
    }

    @Bean
    CommandLineRunner testDB(CryptoRepository repo) {
        return args -> {
            CryptoPrice btc = new CryptoPrice();
            btc.setSymbol("BTC");
            btc.setPrice(65000.0);
            btc.setMarketCap(1000000.0);
            btc.setVolume(50000.0);
            btc.setTimestamp(System.currentTimeMillis());

            repo.save(btc);

            System.out.println("DATA INSERTED");
        };
    }

    /*
    @Bean
    CommandLineRunner testAPI(CryptoApiClient client) {
        return args -> {
            var data = client.fetchCryptoData();

            System.out.println("DATA FROM API:");
            System.out.println(data[0].getSymbol());
        };
    }
     */

    @Bean
    CommandLineRunner run(CryptoService service) {
        return args -> {
            service.fetchAndSaveCryptoData();
            System.out.println("CRYPTO DATA SAVED");
        };
    }
}
