package main

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import (
	"flag"
	"fmt"
	"os"

	"github.com/apache/trafficcontrol/lib/go-log"
	"github.com/apache/trafficcontrol/traffic_diverter/config"
)

func main() {
	configFilePathFlag := flag.String("cfg", "", "The config file path")
	flag.Parse()
	configFilePath := *configFilePathFlag
	if configFilePath == "" {
		flag.Usage()
		os.Exit(1)
	}
	cfg, err := config.Load(configFilePath)
	if err != nil {
		_, _ = fmt.Fprintf(os.Stderr, "Loading Config: %v\n", err)
		os.Exit(1)
	}
	if err := log.InitCfg(cfg); err != nil {
		fmt.Printf("Error initializing loggers: %v\n", err)
		os.Exit(1)
	}
}
