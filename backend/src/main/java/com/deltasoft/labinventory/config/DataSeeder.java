package com.deltasoft.labinventory.config;

import com.deltasoft.labinventory.domain.HazardClass;
import com.deltasoft.labinventory.domain.Reagent;
import com.deltasoft.labinventory.repository.ReagentRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Configuration
@Profile("!test")
public class DataSeeder {

    @Bean
    CommandLineRunner seed(ReagentRepository repo) {
        return args -> {
            if (repo.count() > 0) return;
            LocalDate today = LocalDate.now();
            List<Reagent> seed = List.of(
                build("Sodium Chloride",  "Sigma-Aldrich",   bd("500"),  "g",  "Lab-1 / Shelf-A / Cab-2", today.plusMonths(14), bd("50"),  "LOT-2024-A042", "7647-14-5", HazardClass.NONE),
                build("Ethanol",          "Fisher Sci",      bd("12.50"),"L",  "Lab-2 / Flammables",     today.plusMonths(8),  bd("5"),   "LOT-2024-E113", "64-17-5",   HazardClass.FLAMMABLE),
                build("Acetone",          "VWR",             bd("3"),    "L",  "Lab-2 / Flammables",     today.plusMonths(6),  bd("5"),   "LOT-2024-C201", "67-64-1",   HazardClass.FLAMMABLE),
                build("Hydrochloric Acid","Sigma-Aldrich",   bd("2.00"), "L",  "Lab-3 / Acid Cabinet",   today.plusMonths(18), bd("2"),   "LOT-2023-H088", "7647-01-0", HazardClass.CORROSIVE),
                build("Methanol",         "Honeywell",       bd("0.50"), "L",  "Lab-2 / Flammables",     today.minusDays(20),  bd("5"),   "LOT-2022-M315", "67-56-1",   HazardClass.FLAMMABLE),
                build("Glucose",          "Thermo Fisher",   bd("250"),  "g",  "Lab-1 / Shelf-B",        today.plusMonths(24), bd("100"), "LOT-2024-G027", "50-99-7",   HazardClass.NONE),
                build("Buffer Solution",  "Bio-Rad",         bd("4"),    "L",  "Lab-4 / Cold Room",      today.plusDays(20),   bd("3"),   "LOT-2024-B441", null,        HazardClass.NONE),
                build("Agarose",          "Lonza",           bd("180"),  "g",  "Lab-4 / Shelf-C",        today.plusMonths(10), bd("50"),  "LOT-2024-A902", "9012-36-6", HazardClass.NONE),
                build("Tris Base",        "Sigma-Aldrich",   bd("1000"), "g",  "Lab-1 / Shelf-A",        today.plusMonths(20), bd("100"), "LOT-2024-T556", "77-86-1",   HazardClass.NONE),
                build("EDTA",             "Thermo Fisher",   bd("3.00"), "g",  "Lab-1 / Shelf-A",        today.minusDays(5),   bd("50"),  "LOT-2023-E019", "60-00-4",   HazardClass.NONE)
            );
            repo.saveAll(seed);
        };
    }

    private static Reagent build(String name, String supplier, BigDecimal qty, String unit,
                                 String location, LocalDate exp, BigDecimal min,
                                 String lotNumber, String casNumber, HazardClass hazardClass) {
        Reagent r = new Reagent(name, supplier, qty, unit, location, exp, min);
        r.setLotNumber(lotNumber);
        r.setCasNumber(casNumber);
        r.setHazardClass(hazardClass);
        return r;
    }

    private static BigDecimal bd(String s) { return new BigDecimal(s); }
}
