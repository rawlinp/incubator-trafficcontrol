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
	"crypto/tls"
	"encoding/base64"
	"errors"
	"flag"
	"fmt"
	"net/http"
	"os"
	"time"

	"github.com/apache/trafficcontrol/lib/go-log"
	"github.com/apache/trafficcontrol/traffic_diverter/config"
	toclient "github.com/apache/trafficcontrol/traffic_ops/v3-client"
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
	trafficOps := GetTrafficOpsSession(cfg)
	if trafficOps == nil {
		// TODO: periodically retry until successful
		os.Exit(1)
	}
	certs := GetCDNCerts(trafficOps, cfg)
	if certs == nil {
		// TODO: periodically retry until successful
		os.Exit(1)
	}

	// TODO: set up http and https handlers. Look into https://golang.org/src/net/http/httputil/reverseproxy.go?s=6664:6739#L202
	httpServer := &http.Server{
		Addr:         cfg.HTTPListener,
		ReadTimeout:  10 * time.Second,
		WriteTimeout: 10 * time.Second,
		IdleTimeout:  10 * time.Minute,
		ErrorLog:     log.Error,
	}
	tlsCfg := &tls.Config{}
	if cfg.TLSConfig != nil {
		tlsCfg = cfg.TLSConfig
	}
	tlsCfg.Certificates = certs
	// TODO: generate and add a default cert

	httpsServer := &http.Server{
		Addr:         cfg.HTTPSListener,
		TLSConfig:    tlsCfg,
		ReadTimeout:  10 * time.Second,
		WriteTimeout: 10 * time.Second,
		IdleTimeout:  10 * time.Minute,
		ErrorLog:     log.Error,
	}

	go func() {
		log.Infof("http server started on %s", cfg.HTTPListener)
		if err := httpServer.ListenAndServe(); err != nil {
			if !errors.Is(err, http.ErrServerClosed) {
				log.Errorf("stopping http server: %v", err)
				os.Exit(1)
			}
		}
	}()

	log.Infof("https server started on %s", cfg.HTTPSListener)
	if err := httpsServer.ListenAndServeTLS("", ""); err != nil {
		if !errors.Is(err, http.ErrServerClosed) {
			log.Errorf("stopping http server: %v", err)
			os.Exit(1)
		}
	}

	// TODO: spawn goroutine to handle SIGHUP and reload config
}

func GetTrafficOpsSession(cfg *config.Config) *toclient.Session {
	clientOpts := toclient.ClientOpts{}
	clientOpts.RequestTimeout = 30 * time.Second
	clientOpts.UserAgent = "traffic_diverter/0.1"
	remoteAddr := "unknown"
	session, reqInf, err := toclient.Login(cfg.TrafficOpsURL, cfg.Username, cfg.Password, clientOpts)
	if reqInf.RemoteAddr != nil {
		remoteAddr = reqInf.RemoteAddr.String()
	}
	if err != nil {
		log.Errorf("error logging into Traffic Ops (addr = %s): %v", remoteAddr, err)
		return nil
	}
	log.Infof("successfully logged into Traffic Ops (addr = %s)", remoteAddr)
	return session
}

func GetCDNCerts(trafficOps *toclient.Session, cfg *config.Config) []tls.Certificate {
	remoteAddr := "unknown"
	sslkeys, reqInf, err := trafficOps.GetCDNSSLKeysWithHdr(cfg.CDNName, nil)
	if reqInf.RemoteAddr != nil {
		remoteAddr = reqInf.RemoteAddr.String()
	}
	if err != nil {
		log.Errorf("error getting SSL keys for CDN %s from Traffic Ops (addr = %s): %v", cfg.CDNName, remoteAddr, err)
		return nil
	}
	log.Infof("successfully retrieved SSL keys for CDN %s from Traffic Ops (addr = %s)", cfg.CDNName, remoteAddr)
	certs := make([]tls.Certificate, 0, len(sslkeys))
	for _, sslkey := range sslkeys {
		decodedCert, err := base64.StdEncoding.DecodeString(sslkey.Certificate.Crt)
		if err != nil {
			log.Errorf("base64 decoding certificate: %v", err)
			return nil
		}
		decodedKey, err := base64.StdEncoding.DecodeString(sslkey.Certificate.Key)
		if err != nil {
			log.Errorf("base64 decoding key: %v", err)
			return nil
		}
		cert, err := tls.X509KeyPair(decodedCert, decodedKey)
		if err != nil {
			log.Errorf("creating X509KeyPair: %v", err)
			return nil
		}
		certs = append(certs, cert)
	}
	return certs
}
