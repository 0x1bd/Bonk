Name:           bonk
Version:        {{VERSION}}
Release:        1%{?dist}
Summary:        Soundboard for Linux
License:        MIT
URL:            https://github.com/kvxd/bonk
Source0:        https://github.com/kvxd/bonk/releases/download/v%{version}/%{name}-%{version}.tar.gz

%global debug_package %{nil}

%description
Soundboard for Linux.

%prep
%setup -q

%build

%install
mkdir -p %{buildroot}/opt/%{name}
cp -r bin lib runtime %{buildroot}/opt/%{name}/

mkdir -p %{buildroot}%{_bindir}
ln -s /opt/%{name}/bin/%{name} %{buildroot}%{_bindir}/%{name}

mkdir -p %{buildroot}%{_datadir}/applications
cp share/applications/%{name}.desktop %{buildroot}%{_datadir}/applications/

mkdir -p %{buildroot}%{_datadir}/icons/hicolor/512x512/apps
cp share/icons/hicolor/512x512/apps/%{name}.png %{buildroot}%{_datadir}/icons/hicolor/512x512/apps/

%files
/opt/%{name}
%{_bindir}/%{name}
%{_datadir}/applications/%{name}.desktop
%{_datadir}/icons/hicolor/512x512/apps/%{name}.png

%changelog
# No %changelog here. CI will add it.