import com.fangjia.sjdbc.ShardingJdbcApplicaiton;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = {ShardingJdbcApplicaiton.class})
public class ZTestWithBoot {

    @Test
    public void test() throws IOException, InterruptedException {


    }


}



