import java.util.List;

public class BatchDataProcessor<T> {

    private final int batchSize;
    private final SqlDataProvider sqlDataProvider;
    private final SqlDataUpdator<T> sqlDataUpdator;

    private BatchDataProcessor(Builder<T> builder) {
        this.batchSize = builder.batchSize;
        this.sqlDataProvider = builder.sqlDataProvider;
        this.sqlDataUpdator = builder.sqlDataUpdator;
    }

    public void process() {
        int offset = 0;
        boolean moreData = true;

        while (moreData) {
            // Fetch data in batches using SqlDataProvider
            List<T> batchData = sqlDataProvider.fetchData(offset, batchSize);
            if (batchData.isEmpty()) {
                moreData = false;
            } else {
                // Transform or clean up the data (step 4)
                List<T> transformedData = transformData(batchData);

                // Update data in the database using SqlDataUpdator
                sqlDataUpdator.updateDataInBatches(transformedData);

                // Move to the next batch
                offset += batchSize;
            }
        }
    }

    private List<T> transformData(List<T> data) {
        // Example transformation logic
        // Perform any necessary data transformation here
        return data;
    }

    public static class Builder<T> {
        private int batchSize = 100;  // Default batch size
        private SqlDataProvider sqlDataProvider;
        private SqlDataUpdator<T> sqlDataUpdator;

        public Builder<T> batchSize(int batchSize) {
            this.batchSize = batchSize;
            return this;
        }

        public Builder<T> sqlDataProvider(SqlDataProvider sqlDataProvider) {
            this.sqlDataProvider = sqlDataProvider;
            return this;
        }

        public Builder<T> sqlDataUpdator(SqlDataUpdator<T> sqlDataUpdator) {
            this.sqlDataUpdator = sqlDataUpdator;
            return this;
        }

        public BatchDataProcessor<T> build() {
            if (sqlDataProvider == null || sqlDataUpdator == null) {
                throw new IllegalStateException("SqlDataProvider and SqlDataUpdator must be provided.");
            }
            return new BatchDataProcessor<>(this);
        }
    }
}
