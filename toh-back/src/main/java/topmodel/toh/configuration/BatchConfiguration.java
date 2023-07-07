package topmodel.toh.configuration;

import java.sql.DriverManager;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

import javax.sql.DataSource;

import org.postgresql.PGConnection;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.data.RepositoryItemReader;
import org.springframework.batch.item.data.builder.RepositoryItemReaderBuilder;
import org.springframework.batch.item.database.BeanPropertyItemSqlParameterSourceProvider;
import org.springframework.batch.item.database.JdbcBatchItemWriter;
import org.springframework.batch.item.database.builder.JdbcBatchItemWriterBuilder;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.batch.item.file.mapping.BeanWrapperFieldSetMapper;
import org.springframework.batch.support.transaction.TransactionAwareProxyFactory;
import org.springframework.boot.jdbc.metadata.HikariDataSourcePoolMetadata;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.PlatformTransactionManager;

import com.zaxxer.hikari.HikariDataSource;

import de.bytefish.pgbulkinsert.PgBulkInsert;
import de.bytefish.pgbulkinsert.mapping.AbstractMapping;
import de.bytefish.pgbulkinsert.util.PostgreSqlUtils;
import jakarta.persistence.EntityManagerFactory;
import topmodel.toh.daos.heroes.HeroDAO;
import topmodel.toh.entities.heroes.Hero;

@Configuration
public class BatchConfiguration {
    @Bean
    public FlatFileItemReader<Hero> csvReader() {
        return new FlatFileItemReaderBuilder<Hero>()
                .name("personItemReader")
                .resource(new ClassPathResource("sample-data.csv"))
                .delimited()
                .names(new String[] { "id", "name" })
                .fieldSetMapper(new BeanWrapperFieldSetMapper<Hero>() {
                    {
                        setTargetType(Hero.class);
                    }
                })
                .build();
    }

    @Bean
    public RepositoryItemReader<Hero> pageReader(EntityManagerFactory entityManagerFactory, HeroDAO heroDAO) {
        var sorts = new HashMap<String, Sort.Direction>();
        sorts.put("id", Sort.Direction.ASC);
        return new RepositoryItemReaderBuilder<Hero>().name("pageReader")
                .repository(heroDAO)
                .methodName("findAll")
                .sorts(sorts)
                .build();
    }

    @Bean
    public Job importUserJob(JobRepository jobRepository, Step step1) {
        return new JobBuilder("importUserJob", jobRepository)
                .incrementer(new RunIdIncrementer())
                .flow(step1)
                .end()
                .build();
    }

    @Bean
    public JdbcBatchItemWriter<Hero> writer(DataSource dataSource) {
        return new JdbcBatchItemWriterBuilder<Hero>()
                .itemSqlParameterSourceProvider(new BeanPropertyItemSqlParameterSourceProvider<>())
                .sql("INSERT INTO hero (her_id, her_name) VALUES (:id, :name)")
                .dataSource(dataSource)
                .build();
    }

    @Bean
    public Step step1(JobRepository jobRepository,
            PlatformTransactionManager transactionManager, JdbcBatchItemWriter<Hero> writer) {
        return new StepBuilder("step1", jobRepository)
                .<Hero, Hero>chunk(100000, transactionManager)
                .reader(csvReader())
                .processor(processor())
                .writer(writer)
                .build();
    }

    @Bean
    public HeroItemProcessor processor() {
        return new HeroItemProcessor();
    }

    @Bean
    public HeroBulkItemWriter heroBulkItemWriter(HikariDataSource dataSource) {
        return new HeroBulkItemWriter(dataSource);
    }

    public class HeroMapping extends AbstractMapping<Hero> {
        public HeroMapping() {
            super("toh", "hero");
            mapLong("her_id", Hero::getId);
            mapText("her_name", Hero::getName);
        }
    }

    public class HeroBulkItemWriter implements ItemWriter<Hero> {

        HikariDataSource dataSource;

        public HeroBulkItemWriter(HikariDataSource dataSource) {
            this.dataSource = dataSource;

        }

        public void write(Chunk<? extends Hero> items) throws Exception {
            var bulkInsert = new PgBulkInsert<>(new HeroMapping());
            try (var connection = dataSource.getConnection()) {
                List<Hero> heroes = (List<Hero>) items.getItems();
                // Now save all entities of a given stream:
                PGConnection pgConnection = PostgreSqlUtils.getPGConnection(connection);
                bulkInsert.saveAll(pgConnection, heroes.stream());
            }
        }
    }

    public class HeroItemProcessor implements ItemProcessor<Hero, Hero> {
        private static long ID = 1000l;

        @Override
        public Hero process(final Hero hero) throws Exception {
            final Hero transformedH = new Hero(ID++, hero.getId() + " " + hero.getName());
            return transformedH;
        }
    }
}
