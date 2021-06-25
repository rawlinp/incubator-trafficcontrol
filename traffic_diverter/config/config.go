package config

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
	"encoding/json"
	"fmt"
	"io/ioutil"

	"github.com/apache/trafficcontrol/lib/go-log"
	"github.com/apache/trafficcontrol/lib/go-tc/tovalidate"
	"github.com/apache/trafficcontrol/lib/go-util"

	validation "github.com/go-ozzo/ozzo-validation"
	"github.com/go-ozzo/ozzo-validation/is"
)

type Config struct {
	Username           string `json:"username"`
	Password           string `json:"password"`
	TrafficOpsURL      string `json:"traffic_ops_url"`
	Insecure           bool   `json:"insecure"`
	CDNName            string `json:"cdn_name"`
	TargetTRHost       string `json:"target_tr_host"`
	HTTPListener       string `json:"http_listener"`
	HTTPSListener      string `json:"https_listener"`
	LogLocationError   string `json:"log_location_error"`
	LogLocationWarning string `json:"log_location_warning"`
	LogLocationInfo    string `json:"log_location_info"`
	LogLocationDebug   string `json:"log_location_debug"`
	LogLocationEvent   string `json:"log_location_event"`
}

// ErrorLog - critical messages
func (c Config) ErrorLog() log.LogLocation {
	return log.LogLocation(c.LogLocationError)
}

// WarningLog - warning messages
func (c Config) WarningLog() log.LogLocation {
	return log.LogLocation(c.LogLocationWarning)
}

// InfoLog - information messages
func (c Config) InfoLog() log.LogLocation { return log.LogLocation(c.LogLocationInfo) }

// DebugLog - troubleshooting messages
func (c Config) DebugLog() log.LogLocation {
	return log.LogLocation(c.LogLocationDebug)
}

// EventLog - access.log high level transactions
func (c Config) EventLog() log.LogLocation {
	return log.LogLocation(c.LogLocationEvent)
}

func Load(confPath string) (*Config, error) {
	confBytes, err := ioutil.ReadFile(confPath)
	if err != nil {
		return nil, fmt.Errorf("reading conf '%s': %v", confPath, err)
	}

	cfg := Config{}
	err = json.Unmarshal(confBytes, &cfg)
	if err != nil {
		return nil, fmt.Errorf("unmarshalling '%s': %v", confPath, err)
	}
	setDefaults(&cfg)
	if err := validate(&cfg); err != nil {
		return nil, fmt.Errorf("config validation error: %v", err)
	}
	return &cfg, nil
}

func setDefaults(c *Config) {
	if c.LogLocationError == "" {
		c.LogLocationError = log.LogLocationStderr
	}
	for _, loc := range []*string{&c.LogLocationWarning, &c.LogLocationInfo, &c.LogLocationDebug, &c.LogLocationEvent} {
		if *loc == "" {
			*loc = log.LogLocationStdout
		}
	}
	if c.HTTPListener == "" {
		c.HTTPListener = ":80"
	}
	if c.HTTPSListener == "" {
		c.HTTPSListener = ":443"
	}
	if c.TargetTRHost == "" {
		c.TargetTRHost = "localhost"
	}
}

func validate(c *Config) error {
	validateErrs := validation.Errors{
		"username":        validation.Validate(c.Username, validation.Required),
		"password":        validation.Validate(c.Password, validation.Required),
		"traffic_ops_url": validation.Validate(c.TrafficOpsURL, validation.Required, is.URL),
		"cdn_name":        validation.Validate(c.CDNName, validation.Required),
		"target_tr_host":  validation.Validate(c.TargetTRHost, is.Host),
	}
	return util.JoinErrs(tovalidate.ToErrors(validateErrs))
}
