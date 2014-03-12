package com.baasbox.metrics;

import static com.codahale.metrics.MetricRegistry.name;

import java.io.File;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

import com.baasbox.BBConfiguration;
import com.baasbox.db.DbHelper;
import com.baasbox.exception.InvalidAppCodeException;
import com.codahale.metrics.Gauge;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;

public class BaasBoxMetric {
	
	public static final String TIMER_UPTIME = "server.uptime.timer";
	public static final String TIMER_REQUESTS="requests.timer";
	//public static final String METER_REQUESTS="requests.meter";
	public static final String COUNTER_REQUESTS_STATUS="requests.counter.";
	public static final String HISTOGRAM_RESPONSE_SIZE="responses.size";
	public static final String GAUGE_MEMORY_MAX_ALLOCABLE="memory.max_allocable";
	public static final String GAUGE_MEMORY_CURRENT_ALLOCATE = "memory.current_allocate";
	public static final String GAUGE_MEMORY_USED = "memory.used";
	public static final String GAUGE_FILESYSTEM_DATAFILE_SPACE_LEFT = "filesystem.datafile.spaceleft";	
	public static final String GAUGE_FILESYSTEM_BACKUPDIR_SPACE_LEFT = "filesystem.backupdir.spaceleft";
	public static final String GAUGE_DB_DATA_SIZE = "orientdb.data.size";
	public static final String GAUGE_DB_DATA_DIRECTORY_SIZE = "orientdb.data.directory.size";
	
	public static MetricRegistry registry=null;

	private static boolean activate=false;

	public static boolean isActivate() {
		return activate;
	}

	public static void start() {
		if (!activate) {
			registry=new MetricRegistry();
			setGauges();
			BaasBoxMetric.activate = true;
		}
	}
	
	public static void stop(){
		BaasBoxMetric.activate = false;
	}

	private static void setGauges() {
		//memory gauges
		registry.register(name(GAUGE_MEMORY_MAX_ALLOCABLE),
				//TODO: since the max available memory does not change, we should use the cached gauge
                new Gauge<Long>() {
                    @Override
                    public Long getValue() {
                    	Runtime rt = Runtime.getRuntime(); 
                		long maxMemory=rt.maxMemory();
                		return maxMemory;
                    }
                });
		
		registry.register(name(GAUGE_MEMORY_CURRENT_ALLOCATE),
                new Gauge<Long>() {
                    @Override
                    public Long getValue() {
                    	Runtime rt = Runtime.getRuntime(); 
        				long totalMemory=rt.totalMemory();
        				return totalMemory;        				
                    }
                });
		
		registry.register(name(GAUGE_MEMORY_USED),
                new Gauge<Long>() {
                    @Override
                    public Long getValue() {
                    	Runtime rt = Runtime.getRuntime(); 
        				long freeMemory=rt.freeMemory();
        				long totalMemory=rt.totalMemory();
        				return totalMemory - freeMemory;
        			}
                });	
		registry.register(name(GAUGE_FILESYSTEM_DATAFILE_SPACE_LEFT),
				new Gauge<Long>() {
					@Override
                    public Long getValue() {
                    	return new File(BBConfiguration.getDBDir()).getFreeSpace();
        			}				
				});
		
		registry.register(name(GAUGE_FILESYSTEM_BACKUPDIR_SPACE_LEFT),
				new Gauge<Long>() {
					@Override
                    public Long getValue() {
                    	return new File(BBConfiguration.getDBBackupDir()).getFreeSpace();
        			}				
				});
		
		//TODO: we should use the cached gauge
		registry.register(name(GAUGE_DB_DATA_SIZE),
				new Gauge<Long>() {
					@Override
                    public Long getValue() {
						boolean opened=false;
						try{
							if (DbHelper.getConnection()==null || DbHelper.getConnection().isClosed()) {
								DbHelper.open(BBConfiguration.getAPPCODE(), BBConfiguration.getBaasBoxAdminUsername(), BBConfiguration.getBaasBoxAdminUsername());
								opened=true;
							}
							return DbHelper.getConnection().getSize();
						} catch (InvalidAppCodeException e) {
							throw new RuntimeException(e);
						}finally{
							if (opened) DbHelper.close(DbHelper.getConnection());
						}
        			}				
				});
		
		//TODO: we should use the cached gauge
		registry.register(name(GAUGE_DB_DATA_DIRECTORY_SIZE),
				new Gauge<Long>() {
					@Override
                    public Long getValue() {
							return FileUtils.sizeOfDirectory(new File (BBConfiguration.getDBDir()));
        			}				
				});
		

	}

	public static class Track {
		
		private static final long startTime = System.currentTimeMillis();
		
		
		public static long getUpTimeinMillis(){
			return System.currentTimeMillis()-startTime;
		}
		
		public static long getStartTime(){
			return startTime;
		}
		
		public static Timer.Context[] startRequest(String method,String uri){
			if (activate){
//				registry.meter(name(METER_REQUESTS)).mark();
				Timer.Context timer1=  registry.timer(name(TIMER_REQUESTS)).time();
				Timer.Context timer2=  registry.timer(name(TIMER_REQUESTS,method,uri)).time();
				return new Timer.Context[] {timer1,timer2};
			}else return new Timer.Context[]{};
		}
		
		
		public static void endRequest(Timer.Context[] timers, int status,String responseSize){
			if (!activate) return;
			if (!StringUtils.isEmpty(responseSize) && ! responseSize.equals("-"))
				registry.histogram(name(HISTOGRAM_RESPONSE_SIZE)).update(Long.parseLong(responseSize));
			for (Timer.Context timer: timers) timer.stop();
			registry.counter(name(COUNTER_REQUESTS_STATUS + status)).inc();
		}		
		
	}
}
