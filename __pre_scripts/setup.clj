#! /usr/bin/env bb


(require '[babashka.http-client :as http])
(require '[babashka.cli :as cli])
(require '[babashka.process :refer [shell]])

(defn download-file [url destination]
  (with-open [in (io/input-stream (java.net.URL. url))
              out (io/output-stream (io/file destination))]
    (io/copy in out)))

(def cli-options {:git-name        {:require true :alias :n}
                  :git-email       {:require true :alias :e}
                  :github-username {:require true :alias :u}})

(def args (cli/parse-opts *command-line-args* {:spec cli-options}))

(def chezmoi-install-command
  ((http/get "https://get.chezmoi.io/lb") :body)) ;; literally just a raw sh script 🤔
 
(try
  (shell {:out :string} "which" "gh")
  (catch clojure.lang.ExceptionInfo e
    (prn "gh command does not exist!!, installing now")
    (download-file "https://github.com/cli/cli/releases/download/v2.40.1/gh_2.40.1_linux_amd64.tar.gz" "./gh.tar.gz")
    ;; TODO: should I take the easy option of just using (shell "tar")??? probably not haha
    ()))

(prn "Setting gits username and email...")
(shell "git" "config" "--global" "user.name" (args :git-name))
(shell "git" "config" "--global" "user.email" (args :git-email))
(shell "gh" "auth" "login")

(let [home-dir (System/getenv "HOME")
      chezmoi-exec (str home-dir "/.local/bin/chezmoi")]
  (prn "Installing chezmoi to $HOME/.local/bin")
  (shell {:dir home-dir} "sh" "-c" chezmoi-install-command)

  (prn (str "Installing \"https://github.com/" (args :github-username) "/dotfiles.git\""))
  (shell "chmod" "+x" chezmoi-exec)
  (shell chezmoi-exec "init" "--apply" (args :github-username)))


